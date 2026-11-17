package com.zjgsu.course.dto;

import com.zjgsu.course.model.Course;
import com.zjgsu.course.model.Instructor;
import com.zjgsu.course.model.ScheduleSlot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public final class CourseDtos {

  private CourseDtos() {}

  public record InstructorDetails(
      @NotBlank String id, @NotBlank String name, @NotBlank @Email String email) {}

  public record ScheduleDetails(
      @NotNull DayOfWeek dayOfWeek, @NotNull LocalTime startTime,
      @NotNull LocalTime endTime) {}

  public record Request(
      @NotBlank String code, @NotBlank String title,
      @Valid @NotNull InstructorDetails instructor,
      @Valid @NotNull ScheduleDetails schedule,
      @Min(1) int capacity) {

    public Course toModel() {
      return new Course(code, title,
          new Instructor(instructor.id(), instructor.name(), instructor.email()),
          new ScheduleSlot(schedule.dayOfWeek(), schedule.startTime(), schedule.endTime()),
          capacity);
    }
  }

  public record Response(
      String id, String code, String title, InstructorDetails instructor,
      ScheduleDetails schedule, int capacity, int enrolled) {

    public static Response from(Course course) {
      Instructor teacher = course.getInstructor();
      ScheduleSlot slot = course.getSchedule();
      return new Response(course.getId(), course.getCode(), course.getTitle(),
          new InstructorDetails(teacher.id(), teacher.name(), teacher.email()),
          new ScheduleDetails(slot.dayOfWeek(), slot.startTime(), slot.endTime()),
          course.getCapacity(), course.getEnrolled());
    }
  }
}
