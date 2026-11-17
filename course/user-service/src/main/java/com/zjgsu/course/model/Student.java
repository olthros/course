package com.zjgsu.course.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "students", uniqueConstraints = @UniqueConstraint(columnNames = "student_number"))
public class Student {

  @Id
  private String id;

  @Column(name = "student_number", nullable = false)
  private String studentNumber;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String major;

  protected Student() {}

  public Student(String studentNumber, String name, String major) {
    this.id = UUID.randomUUID().toString();
    this.studentNumber = studentNumber;
    this.name = name;
    this.major = major;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getStudentNumber() {
    return studentNumber;
  }

  public String getName() {
    return name;
  }

  public String getMajor() {
    return major;
  }

  public void update(String studentNumber, String name, String major) {
    this.studentNumber = studentNumber;
    this.name = name;
    this.major = major;
  }
}
