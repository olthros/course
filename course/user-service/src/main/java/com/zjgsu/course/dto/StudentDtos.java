package com.zjgsu.course.dto;

import com.zjgsu.course.model.Student;
import jakarta.validation.constraints.NotBlank;

public final class StudentDtos {

  private StudentDtos() {}

  public record Request(
      @NotBlank String studentNumber, @NotBlank String name, @NotBlank String major) {

    public Student toModel() {
      return new Student(studentNumber, name, major);
    }
  }

  public record Response(String id, String studentNumber, String name, String major) {

    public static Response from(Student student) {
      return new Response(student.getId(), student.getStudentNumber(),
          student.getName(), student.getMajor());
    }
  }
}
