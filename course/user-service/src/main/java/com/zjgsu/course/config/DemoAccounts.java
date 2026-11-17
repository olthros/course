package com.zjgsu.course.config;

import com.zjgsu.course.model.Account;
import com.zjgsu.course.model.Student;
import com.zjgsu.course.repository.AccountRepository;
import com.zjgsu.course.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DemoAccounts {
  @Bean
  CommandLineRunner initializeAccounts(AccountRepository accounts, StudentRepository students,
      @Value("${DEMO_STUDENT_PASSWORD:}") String studentPassword,
      @Value("${DEMO_ADMIN_PASSWORD:}") String adminPassword) {
    return args -> {
      var encoder = new BCryptPasswordEncoder();
      if (!studentPassword.isBlank() && accounts.findByUsername("student1").isEmpty()) {
        var student = students.findByStudentNumber("S001")
            .orElseGet(() -> students.save(new Student("S001", "Student One", "Computer Science")));
        accounts.save(new Account("student1", encoder.encode(studentPassword), "STUDENT", student.getId()));
      }
      if (!adminPassword.isBlank() && accounts.findByUsername("admin").isEmpty()) {
        accounts.save(new Account("admin", encoder.encode(adminPassword), "ADMIN", null));
      }
    };
  }
}
