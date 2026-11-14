package com.zjgsu.course.controller;

import com.zjgsu.course.dto.StudentDtos;
import com.zjgsu.course.service.StudentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
public class StudentController {

  private final StudentService students;

  public StudentController(StudentService students) {
    this.students = students;
  }

  @PostMapping
  public ResponseEntity<StudentDtos.Response> create(
      @Valid @RequestBody StudentDtos.Request request) {
    StudentDtos.Response response = StudentDtos.Response.from(students.createStudent(request.toModel()));
    return ResponseEntity.created(URI.create("/api/students/" + response.id())).body(response);
  }

  @GetMapping
  public List<StudentDtos.Response> findAll() {
    return students.findAll().stream().map(StudentDtos.Response::from).toList();
  }

  @GetMapping("/{id}")
  public StudentDtos.Response findById(@PathVariable String id) {
    return StudentDtos.Response.from(students.findById(id));
  }

  @PutMapping("/{id}")
  public StudentDtos.Response update(@PathVariable String id,
      @Valid @RequestBody StudentDtos.Request request) {
    return StudentDtos.Response.from(students.updateStudent(id, request.toModel()));
  }
}
