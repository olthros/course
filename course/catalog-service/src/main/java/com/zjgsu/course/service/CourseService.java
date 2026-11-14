package com.zjgsu.course.service;

import com.zjgsu.course.exception.ResourceNotFoundException;
import com.zjgsu.course.model.Course;
import com.zjgsu.course.repository.CourseRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CourseService {

  private final CourseRepository courseRepository;

  public CourseService(CourseRepository courseRepository) {
    this.courseRepository = courseRepository;
  }

  public Course createCourse(Course course) {
    if (courseRepository.findByCode(course.getCode()).isPresent()) {
      throw new IllegalStateException("课程代码已存在: " + course.getCode());
    }
    return courseRepository.save(course);
  }

  public Course findById(String id) {
    return courseRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("课程不存在: " + id));
  }

  public List<Course> findAll() {
    return courseRepository.findAll();
  }

  public Course updateCourse(String id, Course changes) {
    Course course = findById(id);
    courseRepository.findByCode(changes.getCode()).ifPresent(existing -> {
      if (!existing.getId().equals(id)) {
        throw new IllegalStateException("课程代码已存在: " + changes.getCode());
      }
    });
    course.update(changes.getCode(), changes.getTitle(), changes.getInstructor(),
        changes.getSchedule(), changes.getCapacity());
    return courseRepository.save(course);
  }

  public void occupySeat(String id) {
    Course course = findById(id);
    course.enrollStudent();
    courseRepository.save(course);
  }

  public void releaseSeat(String id) {
    Course course = findById(id);
    course.withdrawStudent();
    courseRepository.save(course);
  }
}
