package com.zjgsu.course.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zjgsu.course.model.Account;
import com.zjgsu.course.repository.AccountRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class AuthControllerTest {
  @Test
  void authenticatesPasswordAndSignsClaims() {
    byte[] bytes = "0123456789012345678901234567890101234567890123456789012345678901"
        .getBytes(StandardCharsets.UTF_8);
    var account = new Account("student1", new BCryptPasswordEncoder().encode("password"), "STUDENT", "s1");
    ReflectionTestUtils.setField(account, "id", 12L);
    var repository = mock(AccountRepository.class);
    when(repository.findByUsername("student1")).thenReturn(Optional.of(account));
    var controller = new AuthController(repository, Base64.getEncoder().encodeToString(bytes));
    var token = controller.login(new AuthController.Login("student1", "password")).token();
    var claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(bytes)).build().parseSignedClaims(token).getPayload();
    assertEquals("12", claims.getSubject());
    assertEquals("STUDENT", claims.get("role"));
    assertEquals("s1", claims.get("studentId"));
    assertThrows(ResponseStatusException.class,
        () -> controller.login(new AuthController.Login("student1", "incorrect")));
  }
}
