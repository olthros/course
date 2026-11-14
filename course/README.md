# Course 微服务（第 08 周）

依据[第 08 周课堂实践](../../../slides/08/practice.qmd)，在第 07 周服务发现基础上改用 OpenFeign，保留原有模型和接口。

| 工程 | 默认端口 | 数据库 | 模型 |
|------|----------|--------|------|
| `user-service/` | 8081 | `user_db` | `Student` |
| `catalog-service/` | 8082 | `catalog_db` | `Course`、`Instructor`、`ScheduleSlot` |
| `enrollment-service/` | 8083 | `enrollment_db` | `Enrollment`、`EnrollmentStatus` |

每个服务只访问自己的 Repository。选课记录只保存 `studentId`、`courseId`，不跨库建外键。
选课服务通过 `RestTemplate` 校验学生并请求课程服务占用或释放名额。
三个服务注册到 Nacos 的 `COURSE_GROUP`；选课服务通过 OpenFeign 和 LoadBalancer 调用学生和课程服务。
不加入配置中心、网关、消息或分布式事务。
版本见[课程技术栈](../../../docs/TECH_STACK.md)。

## 启动与检查

在本目录执行：

```bash
cp .env.example .env
# 编辑 .env，设置数据库密码、Nacos 管理员密码 NACOS_PASSWORD
# 将以下命令的输出分别填入 NACOS_AUTH_TOKEN、NACOS_AUTH_IDENTITY_VALUE
openssl rand -base64 32
openssl rand -hex 16
docker compose up -d nacos
```

等待 Nacos 启动，打开 <http://localhost:18080/>，初始化 `nacos` 管理员密码，
必须与 `.env` 中的 `NACOS_PASSWORD` 一致。然后启动应用：

```bash
docker compose up -d --build
docker compose ps
bash scripts/smoke-test.sh
```

等待三个数据库健康、三个应用启动后再测试。脚本失败时用 `docker compose logs` 查看启动情况后重试。
应用等待 Nacos 管理员登录可用后启动；首次启动必须先初始化管理员。
控制台使用 18080 映射容器 8080，HTTP API 使用 8848，客户端 gRPC 使用 9848；服务连接 `nacos:8848`。
MySQL 不发布宿主机端口，三个服务分别使用独立账号和数据卷。
若与 Todo 同时运行，请在 `.env` 中将 `USER_PORT`、`CATALOG_PORT`、`ENROLLMENT_PORT` 改成空闲端口，
同时更改 `NACOS_CONSOLE_PORT`、`NACOS_HTTP_PORT`、`NACOS_GRPC_PORT`（例如 28080、18848、19848），
并相应设置健康检查脚本的 `USER_URL`、`CATALOG_URL`、`ENROLLMENT_URL`、`NACOS_CONSOLE_URL`。
容器网络内的端口不变；本机 SDK 使用映射端口时，gRPC 映射端口须等于 HTTP 端口加 1000。

## 顺序验证选课与退课

创建学生：

```bash
curl -i http://localhost:8081/api/students \
  -H 'Content-Type: application/json' \
  -d '{"studentNumber":"20260001","name":"Zhang San","major":"Software Engineering"}'
```

创建容量为 1 的课程：

```bash
curl -i http://localhost:8082/api/courses \
  -H 'Content-Type: application/json' \
  -d '{
    "code":"MS2026","title":"Microservices",
    "instructor":{"id":"T001","name":"Teacher","email":"teacher@example.com"},
    "schedule":{"dayOfWeek":"MONDAY","startTime":"08:00","endTime":"09:35"},
    "capacity":1
  }'
```

分别复制响应的 `id`，然后选课：

```bash
STUDENT_ID='替换为学生 id'
COURSE_ID='替换为课程 id'
curl -i http://localhost:8083/api/enrollments \
  -H 'Content-Type: application/json' \
  -d "{\"studentId\":\"$STUDENT_ID\",\"courseId\":\"$COURSE_ID\"}"
curl "http://localhost:8083/api/enrollments/student/$STUDENT_ID"
curl "http://localhost:8082/api/courses/$COURSE_ID"
```

预期选课返回 201，学生有一条记录，课程 `enrolled` 为 1。
同一学生重复选课返回 409；再创建另一位学生，选择已满课程也返回 409；不存在的学生或课程返回 404。
复制选课响应的 `id` 后退课：

```bash
ENROLLMENT_ID='替换为选课记录 id'
curl -i -X DELETE "http://localhost:8083/api/enrollments/$ENROLLMENT_ID"
curl "http://localhost:8083/api/enrollments/student/$STUDENT_ID"
curl "http://localhost:8082/api/courses/$COURSE_ID"
```

预期退课返回 204，选课列表为空、课程 `enrolled` 为 0，之后可重新选课。
学生和课程原有的列表、按 ID 查询接口仍保留。

## 内部调用与失败边界

| 调用 | 作用 |
|------|------|
| `GET /api/students/{id}` | 校验学生 |
| `POST /api/courses/{id}/seats` | 占用一个名额，成功 204，满员 409 |
| `DELETE /api/courses/{id}/seats` | 释放一个名额，成功 204 |

选课服务先检查重复，再校验学生、占用名额，最后保存本地记录；退课先释放名额，再删除本地记录。
无效请求返回 400；远端 404/409 传回客户端；连接失败、超时或远端服务故障返回 503。

**这是顺序业务基线，不是生产级一致性方案：**

- `@Transactional` 只覆盖本服务数据库。远端占用名额后本地保存失败，会留下计数差异；退课也有相同窗口。
- 写请求超时可能意味着远端已提交，不能据此断定“没有执行”。占用/释放名额不是幂等请求，不应盲目重试。
- 当前没有锁、请求去重或自动补偿，不保证并发选课不超卖，不能将顺序满员测试当成并发正确性证明。
- 内部名额接口用于服务协作，直接调用可能造成计数与选课记录不符；本周尚未引入认证或网关。

## 测试与数据库隔离

```bash
bash scripts/test.sh
```

测试使用 H2，关闭真实注册并模拟客户端，不需要启动 Nacos。选课测试只注入本地选课 Repository，验证依赖故障、满员、重复选课、
退课和远端写入后响应超时；不再注入另两个服务的 Repository。Docker 构建也运行测试。

可分别进入数据库，用该服务在 `.env` 中配置的密码执行 `SHOW TABLES;`：

```bash
docker compose exec user-db mysql -ucourse -p user_db
docker compose exec catalog-db mysql -ucourse -p catalog_db
docker compose exec enrollment-db mysql -ucourse -p enrollment_db
```

应分别只有学生、课程、选课相关的表。
直接运行 Maven 时，为各工程提供 `DB_URL`、`DB_USER`、`DB_PASSWORD`，然后执行 `./mvnw spring-boot:run`；
还需设置 `NACOS_SERVER_ADDR`（默认 `localhost:8848`）、`NACOS_USERNAME`（默认 `nacos`）、
`NACOS_PASSWORD`。Maven 不自动读取根目录 `.env`。
`NACOS_NAMESPACE` 留空表示默认命名空间；自定义时须先在控制台创建，并填入 ID 而非显示名称。
三个服务的 Namespace 和 Group 必须一致；全容器与全本机运行任选其一，勿混用不可互访的注册地址。

## 注册与故障验证

在控制台的默认命名空间、`COURSE_GROUP` 中确认三个服务各有健康实例。
健康检查和注册列表只用于辅助判断，须完成选课与退课请求才能验证业务调用链。
业务 HTTP 请求由服务直接发送，不经过 Nacos 转发。

1. 停止 `user-service`，等实例下线后，为尚未选课的学生提交选课应返回 503，不产生选课记录。
2. 恢复用户服务、停止 `catalog-service` 后，选课和已有记录的退课应返回 503，原有选课记录应保留。
3. 恢复目录服务，等重新注册后再次选课、退课应成功，无须修改选课服务地址。

使用 `docker compose stop <服务名>`、`docker compose start <服务名>` 操作。
SDK 可能短暂保留旧地址，连接失败仍应返回 503；不能把不可用解释成“学生或课程不存在”。
若注册存在但调用失败，检查实例 IP/业务端口能否互访、Namespace 和 Group 是否一致。
不自动重试写请求。

## 观察负载均衡

启动并创建一位学生后，使用覆盖文件启动两个课程服务：

```bash
docker compose -f docker-compose.yml -f compose.scale.yml up -d --scale catalog-service=2
for i in {1..10}; do
  curl -i http://localhost:8083/api/enrollments \
    -H 'Content-Type: application/json' \
    -d "{\"studentId\":\"$STUDENT_ID\",\"courseId\":\"missing-course\"}"
done
docker compose -f docker-compose.yml -f compose.scale.yml logs catalog-service
```

请求预期返回 404；检查两个实例的请求日志和 Nacos 健康实例列表。
覆盖文件取消课程服务的宿主机端口映射；运行结束后执行
`docker compose up -d --scale catalog-service=1` 恢复。

`docker compose down` 保留数据；`docker compose down -v` 会清空本实验数据卷，使用前确认无需保留。
已有数据卷的 MySQL 密码不会随 `.env` 自动修改。
Nacos 管理员密码也不会随 `.env` 自动修改，需在控制台同步修改。

## 熔断扩展：学生查询与课程名额

本版本为选课服务的 Feign 方法分别配置熔断器：

| 调用 | 熔断器指标中的 name |
| --- | --- |
| 学生查询 | `StudentClientfindByIdString` |
| 占用名额 | `CatalogClientoccupyString` |
| 释放名额 | `CatalogClientreleaseString` |

各方法独立统计最近 10 次调用，至少调用 5 次且失败率达到 50% 时打开，
等待 60 秒后允许 3 次试探调用。400、404、409 等预期业务错误保留原状态码，
不计入失败率；服务故障或熔断器打开时返回 503，不伪造学生、名额或选课成功。
本教学示例关闭额外隔离、线程池和 TimeLimiter，保留 Feign 超时。

熔断不解决远程名额修改与本地选课事务的一致性问题。写调用超时可能已经生效，
本例不自动重试；请核实状态，不能把 503 理解为远端一定没有修改。

沿用前文创建的 `STUDENT_ID` 和 `COURSE_ID`；请选择尚未选过且有剩余名额的课程。
在同一 Bash 终端设置：

```bash
BASE_URL=${ENROLLMENT_URL:-http://localhost:8083}
METRICS_URL="$BASE_URL"
AUTH=()
```

停止学生服务并连续请求。学生不可用时不会保存选课记录，故可重复使用同一课程：

```bash
docker compose stop user-service
for i in {1..7}; do
  curl -sS -o /dev/null -w '%{http_code}\n' "${AUTH[@]}" \
    "$BASE_URL/api/enrollments" -H 'Content-Type: application/json' \
    -d "{\"studentId\":\"$STUDENT_ID\",\"courseId\":\"$COURSE_ID\"}"
done
curl -sG "$METRICS_URL/actuator/metrics/resilience4j.circuitbreaker.state" \
  --data-urlencode 'tag=name:StudentClientfindByIdString' \
  --data-urlencode 'tag=state:open'
```

请求返回 503，OPEN 指标的 `value` 为 1.0 才证明熔断器已经打开。
执行 `docker compose start user-service`，等待服务在 Nacos 恢复健康，
并等熔断打开满 60 秒后，再进行三次选课、退课；JWT 过期时先重新登录。

```bash
for i in {1..3}; do
  ENROLLMENT_ID=$(curl -fsS "${AUTH[@]}" "$BASE_URL/api/enrollments" \
    -H 'Content-Type: application/json' \
    -d "{\"studentId\":\"$STUDENT_ID\",\"courseId\":\"$COURSE_ID\"}" \
    | jq -er '.id') || break
  curl -fsS "${AUTH[@]}" -X DELETE \
    "$BASE_URL/api/enrollments/$ENROLLMENT_ID" || break
done
curl -sG "$METRICS_URL/actuator/metrics/resilience4j.circuitbreaker.state" \
  --data-urlencode 'tag=name:StudentClientfindByIdString' \
  --data-urlencode 'tag=state:closed'
```

成功创建返回 201，退课返回 204；CLOSED 指标的 `value` 为 1.0。
退课是为了避免重复选课的 409 阻止后续请求到达学生服务。
这些请求会实际修改数据，后续周次还可能产生通知，仅在教学环境操作。

课程服务的故障可用 `docker compose stop catalog-service` 演示，
然后查看占用名额方法的熔断指标；结束后务必恢复服务。
占用、释放和容量查询分别熔断，不能用重复选课、名额不足等业务错误来触发服务熔断。
