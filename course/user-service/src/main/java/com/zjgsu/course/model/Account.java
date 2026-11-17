package com.zjgsu.course.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class Account {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private String role;

  private String studentId;

  protected Account() {}

  public Account(String username, String passwordHash, String role, String studentId) {
    this.username = username;
    this.passwordHash = passwordHash;
    this.role = role;
    this.studentId = studentId;
  }

  public Long getId() { return id; }
  public String getUsername() { return username; }
  public String getPasswordHash() { return passwordHash; }
  public String getRole() { return role; }
  public String getStudentId() { return studentId; }
}
