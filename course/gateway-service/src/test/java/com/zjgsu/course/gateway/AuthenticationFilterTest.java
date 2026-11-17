package com.zjgsu.course.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class AuthenticationFilterTest {
  private final byte[] bytes = "0123456789012345678901234567890101234567890123456789012345678901"
      .getBytes(StandardCharsets.UTF_8);
  private final AuthenticationFilter filter = new AuthenticationFilter(Base64.getEncoder().encodeToString(bytes));

  private String token(String role, Instant expiration) {
    return Jwts.builder().subject("12").claim("username", "student1").claim("role", role)
        .expiration(Date.from(expiration)).signWith(Keys.hmacShaKeyFor(bytes), Jwts.SIG.HS512).compact();
  }

  @Test
  void requiresValidToken() {
    var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/students"));
    filter.filter(exchange, ignored -> Mono.empty()).block();
    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());

    var expired = MockServerWebExchange.from(MockServerHttpRequest.get("/api/students")
        .header("Authorization", "Bearer " + token("STUDENT", Instant.now().minusSeconds(60))));
    filter.filter(expired, ignored -> Mono.empty()).block();
    assertEquals(HttpStatus.UNAUTHORIZED, expired.getResponse().getStatusCode());
  }

  @Test
  void rejectsStudentCreatingCourse() {
    var exchange = MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.POST, "/api/courses")
        .header("Authorization", "Bearer " + token("STUDENT", Instant.now().plusSeconds(60))));
    filter.filter(exchange, ignored -> Mono.empty()).block();
    assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
  }

  @Test
  void blocksInternalSeatEndpoint() {
    var exchange = MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.POST, "/api/courses/42/seats")
        .header("Authorization", "Bearer " + token("STUDENT", Instant.now().plusSeconds(60))));
    filter.filter(exchange, ignored -> Mono.empty()).block();
    assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
  }

  @Test
  void replacesUntrustedIdentityHeaders() {
    var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/students")
        .header("Authorization", "Bearer " + token("STUDENT", Instant.now().plusSeconds(60)))
        .header("X-User-Role", "ADMIN").header("X-Student-Id", "forged"));
    filter.filter(exchange, forwarded -> {
      assertEquals("STUDENT", forwarded.getRequest().getHeaders().getFirst("X-User-Role"));
      assertNull(forwarded.getRequest().getHeaders().getFirst("X-Student-Id"));
      return Mono.empty();
    }).block();
  }
}
