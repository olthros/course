package com.zjgsu.course.controller;

import com.zjgsu.course.repository.AccountRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AccountRepository accounts;
  private final String secret;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public AuthController(AccountRepository accounts, @Value("${JWT_SECRET:}") String secret) {
    this.accounts = accounts;
    this.secret = secret;
  }

  @PostMapping("/login")
  public Token login(@RequestBody Login request) {
    if (request.username() == null || request.username().isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    var account = accounts.findByUsername(request.username())
        .filter(a -> request.password() != null && encoder.matches(request.password(), a.getPasswordHash()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    Instant now = Instant.now();
    var builder = Jwts.builder().subject(account.getId().toString())
        .claim("username", account.getUsername()).claim("role", account.getRole())
        .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(3600)));
    if (account.getStudentId() != null) {
      builder.claim("studentId", account.getStudentId());
    }
    String token = builder.signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)), Jwts.SIG.HS512).compact();
    return new Token(token, account.getUsername(), account.getRole());
  }

  public record Login(String username, String password) {}
  public record Token(String token, String username, String role) {}
}
