package com.zjgsu.course.controller;

import com.zjgsu.course.dto.CourseDtos;
import com.zjgsu.course.service.CourseService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

  private final CourseService courses;

  public CourseController(CourseService courses) {
    this.courses = courses;
  }

  @PostMapping
  public ResponseEntity<CourseDtos.Response> create(
      @Valid @RequestBody CourseDtos.Request request) {
    CourseDtos.Response response = CourseDtos.Response.from(courses.createCourse(request.toModel()));
    return ResponseEntity.created(URI.create("/api/courses/" + response.id())).body(response);
  }

  @GetMapping
  public List<CourseDtos.Response> findAll() {
    return courses.findAll().stream().map(CourseDtos.Response::from).toList();
  }

  @GetMapping("/{id}")
  public CourseDtos.Response findById(@PathVariable String id) {
    return CourseDtos.Response.from(courses.findById(id));
  }

  @PutMapping("/{id}")
  public CourseDtos.Response update(@PathVariable String id,
      @Valid @RequestBody CourseDtos.Request request) {
    return CourseDtos.Response.from(courses.updateCourse(id, request.toModel()));
  }

  @PostMapping("/{id}/seats")
  public ResponseEntity<Void> occupySeat(@PathVariable String id) {
    courses.occupySeat(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}/seats")
  public ResponseEntity<Void> releaseSeat(@PathVariable String id) {
    courses.releaseSeat(id);
    return ResponseEntity.noContent().build();
  }
}
