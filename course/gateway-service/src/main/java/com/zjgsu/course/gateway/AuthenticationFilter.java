package com.zjgsu.course.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {
  private final SecretKey key;

  public AuthenticationFilter(@Value("${JWT_SECRET}") String secret) {
    key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    if (key.getEncoded().length < 64) {
      throw new IllegalArgumentException("JWT_SECRET must decode to at least 64 bytes");
    }
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    var request = exchange.getRequest();
    String path = request.getPath().value();
    if (path.matches("^/api/courses/[^/]+/seats/?$")) {
      return reject(exchange, HttpStatus.FORBIDDEN);
    }
    var clean = request.mutate().headers(h -> {
      h.remove("X-User-Id");
      h.remove("X-Username");
      h.remove("X-User-Role");
      h.remove("X-Student-Id");
    });
    if (HttpMethod.POST.equals(request.getMethod()) && path.equals("/api/auth/login")) {
      return chain.filter(exchange.mutate().request(clean.build()).build());
    }
    String authorization = request.getHeaders().getFirst("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return reject(exchange, HttpStatus.UNAUTHORIZED);
    }
    try {
      Claims claims = Jwts.parser().verifyWith(key).build()
          .parseSignedClaims(authorization.substring(7)).getPayload();
      String role = claims.get("role", String.class);
      if (claims.getSubject() == null || claims.getExpiration() == null
          || claims.get("username", String.class) == null || role == null) {
        return reject(exchange, HttpStatus.UNAUTHORIZED);
      }
      if (HttpMethod.POST.equals(request.getMethod())
          && ((path.equals("/api/courses") && !role.equals("ADMIN"))
          || (path.equals("/api/enrollments") && !role.equals("STUDENT")))) {
        return reject(exchange, HttpStatus.FORBIDDEN);
      }
      clean.header("X-User-Id", claims.getSubject());
      clean.header("X-Username", claims.get("username", String.class));
      clean.header("X-User-Role", role);
      String studentId = claims.get("studentId", String.class);
      if (studentId != null) clean.header("X-Student-Id", studentId);
      return chain.filter(exchange.mutate().request(clean.build()).build());
    } catch (JwtException | IllegalArgumentException e) {
      return reject(exchange, HttpStatus.UNAUTHORIZED);
    }
  }

  private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
    exchange.getResponse().setStatusCode(status);
    return exchange.getResponse().setComplete();
  }

  @Override
  public int getOrder() { return -100; }
}
