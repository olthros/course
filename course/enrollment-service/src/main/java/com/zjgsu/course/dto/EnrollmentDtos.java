package com.zjgsu.course.dto;

import com.zjgsu.course.model.Enrollment;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public final class EnrollmentDtos {

  private EnrollmentDtos() {}

  public record Request(@NotBlank String studentId, @NotBlank String courseId) {}

  public record Response(
      String id, String studentId, String courseId, LocalDateTime enrolledAt,
      String status) {

    public static Response from(Enrollment enrollment) {
      return new Response(enrollment.getId(), enrollment.getStudentId(),
          enrollment.getCourseId(), enrollment.getEnrolledAt(),
          enrollment.getStatus().name());
    }
  }
}
