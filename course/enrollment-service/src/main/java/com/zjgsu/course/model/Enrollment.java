package com.zjgsu.course.model;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(
    columnNames = {"student_id", "course_id"}))
public class Enrollment {

  @Id
  private String id;

  @Column(name = "student_id", nullable = false)
  private String studentId;

  @Column(name = "course_id", nullable = false)
  private String courseId;

  @Column(nullable = false)
  private LocalDateTime enrolledAt;

  @Enumerated(EnumType.STRING)
  private EnrollmentStatus status;

  protected Enrollment() {}

  public Enrollment(String studentId, String courseId) {
    this.id = UUID.randomUUID().toString();
    this.studentId = studentId;
    this.courseId = courseId;
    this.enrolledAt = LocalDateTime.now();
    this.status = EnrollmentStatus.ACTIVE;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getStudentId() {
    return studentId;
  }

  public String getCourseId() {
    return courseId;
  }

  public LocalDateTime getEnrolledAt() {
    return enrolledAt;
  }

  public EnrollmentStatus getStatus() {
    return status;
  }

  public void setStatus(EnrollmentStatus status) {
    this.status = status;
  }
}
