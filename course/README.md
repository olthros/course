# 选课微服务：第 09 周

在第 08 周服务通信的基础上增加 API 网关、账号登录和 JWT 认证。
三个业务服务仍使用独立数据库，并注册到 Nacos 的 `COURSE_GROUP`。
业务入口为 `gateway-service` 的 8080 端口；业务服务和数据库不向宿主机发布端口。

## 启动

需要 Docker Compose、curl、jq 和 openssl。在本目录执行：

```bash
cp .env.example .env
openssl rand -base64 64 | tr -d '\n'; echo
openssl rand -base64 32
openssl rand -hex 16
```

编辑 `.env`：依次将上述输出填入 `JWT_SECRET`、`NACOS_AUTH_TOKEN`
和 `NACOS_AUTH_IDENTITY_VALUE`，并填写数据库密码、`NACOS_PASSWORD`、
`DEMO_STUDENT_PASSWORD` 和 `DEMO_ADMIN_PASSWORD`。不要提交 `.env`。

```bash
docker compose up -d nacos
```

首次启动时访问 <http://localhost:18080/> 初始化 Nacos 管理员，密码必须与
`NACOS_PASSWORD` 一致。随后执行：

```bash
docker compose up -d --build
docker compose ps
```

等待服务启动和注册后，在当前 shell 中设置上述两个演示账号密码，并执行：

```bash
export DEMO_STUDENT_PASSWORD='替换为学生密码'
export DEMO_ADMIN_PASSWORD='替换为管理员密码'
bash scripts/smoke-test.sh
```

首次启动用户服务时会创建 `student1` 和 `admin`，密码仅以 BCrypt 哈希保存。
学生账号关联学生编号为 `S001` 的记录。已有账号不会因环境变量变更而重设密码。
若与 Todo 同时运行，调整 `GATEWAY_PORT` 和 Nacos 的三个宿主机端口。

## 登录与访问

```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"student1","password":"替换为学生密码"}'
curl -i http://localhost:8080/api/students \
  -H 'Authorization: Bearer 替换为返回的 token'
```

未登录访问受保护接口返回 401；学生创建课程返回 403，管理员通过角色检查。
网关清除客户端的身份请求头，再写入经过签名验证的身份。课程名额的增减仍由
选课服务通过内部网络调用；这些调用及本地数据库事务的失败边界与第 08 周相同。

本示例展示网关认证，不是完整的生产安全方案：下游尚需资源级授权、服务身份验证
及网络策略，不能仅凭关闭宿主机端口信任内部网络。不要在截图中泄露令牌或密码。

## 测试

```bash
bash scripts/test.sh
```

测试使用 H2 和模拟依赖，不需要运行 Nacos。依赖版本见
[课程技术栈](../../../docs/TECH_STACK.md)；完整的第 08 周步骤可查看 `2026-W08-service-communication`。

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

按前文完成启动，创建一门有剩余名额的课程并记录 `COURSE_ID`。
使用学生账号登录，在同一 Bash 终端保存 Token 与学生 ID：

```bash
BASE_URL=${GATEWAY_URL:-http://localhost:8080}
METRICS_URL=http://localhost:${CIRCUITBREAKER_PORT:-18083}
LOGIN_BODY=$(jq -n --arg password "$DEMO_STUDENT_PASSWORD" \
  '{username:"student1",password:$password}')
TOKEN=$(curl -fsS "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' -d "$LOGIN_BODY" | jq -er '.token')
AUTH=(-H "Authorization: Bearer $TOKEN")
STUDENT_ID=$(curl -fsS "${AUTH[@]}" "$BASE_URL/api/students" \
  | jq -er '.[] | select(.studentNumber == "S001") | .id')
docker compose -f docker-compose.yml -f compose.circuitbreaker.yml \
  up -d --build enrollment-service
```

覆盖文件仅向本机开放指标端口，端口冲突时设置 `CIRCUITBREAKER_PORT`。
业务请求仍通过网关。等待选课服务就绪后再继续。

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
演示结束执行 `docker compose up -d enrollment-service`，移除额外的本机指标端口。

课程服务的故障可用 `docker compose stop catalog-service` 演示，
然后查看占用名额方法的熔断指标；结束后务必恢复服务。
占用、释放和容量查询分别熔断，不能用重复选课、名额不足等业务错误来触发服务熔断。
