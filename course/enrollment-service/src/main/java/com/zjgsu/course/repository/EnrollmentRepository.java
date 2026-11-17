package com.zjgsu.course.repository;

import com.zjgsu.course.model.Enrollment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, String> {

  boolean existsByStudentIdAndCourseId(String studentId, String courseId);

  List<Enrollment> findByStudentId(String studentId);

  List<Enrollment> findByCourseId(String courseId);
}
