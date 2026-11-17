package com.zjgsu.course.repository;

import com.zjgsu.course.model.Course;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, String> {

  Optional<Course> findByCode(String code);
}
