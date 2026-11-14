package com.zjgsu.course.service;

import static org.junit.jupiter.api.Assertions.*;

import com.zjgsu.course.model.Course;
import com.zjgsu.course.model.Instructor;
import com.zjgsu.course.model.ScheduleSlot;
import com.zjgsu.course.repository.CourseRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CourseServiceTest {

  @Autowired CourseService courses;
  @Autowired CourseRepository repository;

  @BeforeEach
  void clear() {
    repository.deleteAll();
  }

  @Test
  void occupiesAndReleasesASeatInSequence() {
    Course course = createCourse();
    courses.occupySeat(course.getId());
    assertEquals(1, courses.findById(course.getId()).getEnrolled());
    assertThrows(IllegalStateException.class, () -> courses.occupySeat(course.getId()));
    assertEquals(1, courses.findById(course.getId()).getEnrolled());
    courses.releaseSeat(course.getId());
    assertEquals(0, courses.findById(course.getId()).getEnrolled());
  }

  @Test
  void rejectsDuplicateCourseCode() {
    createCourse();
    assertThrows(IllegalStateException.class, this::createCourse);
    assertEquals(1, repository.count());
  }

  private Course createCourse() {
    return courses.createCourse(new Course("CS606", "Microservices",
        new Instructor("T1", "Teacher", "teacher@example.edu"),
        new ScheduleSlot(DayOfWeek.MONDAY, LocalTime.of(13, 40), LocalTime.of(15, 15)), 1));
  }
}
