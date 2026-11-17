package com.zjgsu.course.controller;

import com.zjgsu.course.dto.EnrollmentDtos;
import com.zjgsu.course.service.EnrollmentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

  private final EnrollmentService enrollments;

  public EnrollmentController(EnrollmentService enrollments) {
    this.enrollments = enrollments;
  }

  @PostMapping
  public ResponseEntity<EnrollmentDtos.Response> create(
      @Valid @RequestBody EnrollmentDtos.Request request,
      @RequestHeader(value = "X-Student-Id", required = false) String studentId) {
    requireStudent(studentId, request.studentId());
    EnrollmentDtos.Response response = EnrollmentDtos.Response.from(
        enrollments.enroll(request.studentId(), request.courseId()));
    return ResponseEntity.created(URI.create("/api/enrollments/" + response.id()))
        .body(response);
  }

  @GetMapping("/student/{studentId}")
  public List<EnrollmentDtos.Response> findByStudent(@PathVariable String studentId,
      @RequestHeader(value = "X-Student-Id", required = false) String caller,
      @RequestHeader(value = "X-User-Role", required = false) String role) {
    if (!"ADMIN".equals(role)) requireStudent(caller, studentId);
    return enrollments.findByStudentId(studentId).stream()
        .map(EnrollmentDtos.Response::from).toList();
  }

  @GetMapping("/course/{courseId}")
  public List<EnrollmentDtos.Response> findByCourse(@PathVariable String courseId,
      @RequestHeader(value = "X-User-Role", required = false) String role) {
    if (!"ADMIN".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return enrollments.findByCourseId(courseId).stream()
        .map(EnrollmentDtos.Response::from).toList();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> withdraw(@PathVariable String id,
      @RequestHeader(value = "X-Student-Id", required = false) String studentId,
      @RequestHeader(value = "X-User-Role", required = false) String role) {
    enrollments.withdrawAuthorized(id, studentId, "ADMIN".equals(role));
    return ResponseEntity.noContent().build();
  }

  private static void requireStudent(String actual, String expected) {
    if (actual == null || !actual.equals(expected)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }
}
