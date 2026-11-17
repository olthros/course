package com.zjgsu.course.service;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import com.zjgsu.course.repository.EnrollmentRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(properties = {
    "spring.config.import=file:src/main/resources/application.yml",
    "spring.datasource.url=jdbc:h2:mem:course;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "resilience4j.circuitbreaker.configs.remote.wait-duration-in-open-state=200ms"
})
class RemoteCircuitBreakerTest {
  private static final AtomicInteger studentStatus = new AtomicInteger();
  private static final AtomicInteger catalogStatus = new AtomicInteger();
  private static final AtomicInteger studentCalls = new AtomicInteger();
  private static final AtomicInteger catalogCalls = new AtomicInteger();
  private static final HttpServer server = startServer();

  @Autowired StudentClient students;
  @Autowired CatalogClient catalog;
  @Autowired EnrollmentService enrollments;
  @Autowired EnrollmentRepository repository;
  @Autowired CircuitBreakerRegistry registry;

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry properties) {
    for (String name : new String[] {"user-service", "catalog-service"}) {
      properties.add("spring.cloud.openfeign.client.config." + name + ".url",
          () -> "http://127.0.0.1:" + server.getAddress().getPort());
    }
  }

  private static HttpServer startServer() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/api/", exchange -> {
        boolean student = exchange.getRequestURI().getPath().startsWith("/api/students/");
        (student ? studentCalls : catalogCalls).incrementAndGet();
        byte[] body = (student ? "{\"id\":\"S1\"}" : "{\"capacity\":10,\"enrolled\":0}")
            .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders((student ? studentStatus : catalogStatus).get(), body.length);
        try (var output = exchange.getResponseBody()) {
          output.write(body);
        }
      });
      server.start();
      return server;
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @BeforeEach
  void reset() {
    registry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    repository.deleteAll();
    studentStatus.set(200);
    catalogStatus.set(200);
    studentCalls.set(0);
    catalogCalls.set(0);
  }

  @AfterAll
  static void stopServer() {
    server.stop(0);
  }

  private void assertStatus(int status, Runnable action) {
    var error = assertThrows(ResponseStatusException.class, action::run);
    assertEquals(status, error.getStatusCode().value());
  }

  @Test
  void studentNotFoundDoesNotTripCircuitBreaker() {
    studentStatus.set(404);
    for (int i = 0; i < 6; i++) {
      assertStatus(404, () -> enrollments.enroll("S1", "C1"));
    }
    CircuitBreaker breaker = registry.circuitBreaker("StudentClientfindByIdString");
    assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    assertEquals(0, breaker.getMetrics().getNumberOfFailedCalls());
    assertEquals(0, repository.count());
    assertEquals(0, catalogCalls.get());
  }

  @Test
  void studentFailureOpensAndRecovers() throws InterruptedException {
    studentStatus.set(503);
    for (int i = 0; i < 5; i++) {
      assertStatus(503, () -> students.findById("S1"));
    }
    CircuitBreaker breaker = registry.circuitBreaker("StudentClientfindByIdString");
    assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
    assertStatus(503, () -> enrollments.enroll("S1", "C1"));
    assertEquals(5, studentCalls.get());
    assertEquals(0, repository.count());

    studentStatus.set(200);
    Thread.sleep(250);
    for (int i = 0; i < 3; i++) {
      assertEquals("S1", students.findById("S1").get("id"));
    }
    assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
  }

  @Test
  void catalogBusinessErrorsAreNotServiceFailures() {
    for (int status : new int[] {400, 404, 409}) {
      catalogStatus.set(status);
      for (int i = 0; i < 6; i++) {
        assertStatus(status, () -> catalog.occupy("C1"));
        assertStatus(status, () -> catalog.release("C1"));
      }
    }
    for (String name : new String[] {"CatalogClientoccupyString", "CatalogClientreleaseString"}) {
      CircuitBreaker breaker = registry.circuitBreaker(name);
      assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
      assertEquals(0, breaker.getMetrics().getNumberOfFailedCalls());
    }
  }

  @Test
  void occupyFailureDoesNotPretendToSucceedOrBlockRelease() throws InterruptedException {
    catalogStatus.set(503);
    for (int i = 0; i < 5; i++) {
      assertStatus(503, () -> catalog.occupy("C1"));
    }
    CircuitBreaker breaker = registry.circuitBreaker("CatalogClientoccupyString");
    assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
    assertStatus(503, () -> enrollments.enroll("S1", "C1"));
    assertEquals(5, catalogCalls.get());
    assertEquals(0, repository.count());

    catalogStatus.set(200);
    catalog.release("C1");
    assertEquals(6, catalogCalls.get());
    Thread.sleep(250);
    for (int i = 0; i < 3; i++) {
      catalog.occupy("C1");
    }
    assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
  }
}
