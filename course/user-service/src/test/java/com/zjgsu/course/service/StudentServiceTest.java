package com.zjgsu.course.service;

import static org.junit.jupiter.api.Assertions.*;

import com.zjgsu.course.model.Student;
import com.zjgsu.course.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StudentServiceTest {

  @Autowired StudentService students;
  @Autowired StudentRepository repository;

  @BeforeEach
  void clear() {
    repository.deleteAll();
  }

  @Test
  void createsAndReadsStudent() {
    Student student = students.createStudent(new Student("2026001", "Student", "Software"));
    assertEquals("2026001", students.findById(student.getId()).getStudentNumber());
  }

  @Test
  void rejectsDuplicateStudentNumber() {
    students.createStudent(new Student("2026001", "First", "Software"));
    assertThrows(IllegalStateException.class,
        () -> students.createStudent(new Student("2026001", "Second", "Software")));
    assertEquals(1, repository.count());
  }
}
