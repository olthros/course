package com.zjgsu.course.service;

import com.zjgsu.course.exception.ResourceNotFoundException;
import com.zjgsu.course.model.Enrollment;
import com.zjgsu.course.repository.EnrollmentRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class EnrollmentService {

  private final EnrollmentRepository enrollmentRepository;
  private final StudentClient students;
  private final CatalogClient catalog;

  public EnrollmentService(EnrollmentRepository enrollmentRepository, StudentClient students,
      CatalogClient catalog) {
    this.enrollmentRepository = enrollmentRepository;
    this.students = students;
    this.catalog = catalog;
  }

  public Enrollment enroll(String studentId, String courseId) {
    if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
      throw new IllegalStateException("学生已经选择该课程");
    }
    Map<?, ?> student = students.findById(studentId);
    if (student == null || !studentId.equals(student.get("id"))) {
      throw new ResourceAccessException("学生服务返回无效响应");
    }
    catalog.occupy(courseId);
    return enrollmentRepository.save(new Enrollment(studentId, courseId));
  }

  public void withdraw(String enrollmentId) {
    Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
        .orElseThrow(() -> new ResourceNotFoundException("选课记录不存在: " + enrollmentId));
    catalog.release(enrollment.getCourseId());
    enrollmentRepository.deleteById(enrollmentId);
  }

  public void withdrawAuthorized(String enrollmentId, String studentId, boolean admin) {
    Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
        .orElseThrow(() -> new ResourceNotFoundException("选课记录不存在: " + enrollmentId));
    if (!admin && (studentId == null || !studentId.equals(enrollment.getStudentId()))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    catalog.release(enrollment.getCourseId());
    enrollmentRepository.deleteById(enrollmentId);
  }

  public List<Enrollment> findByStudentId(String studentId) {
    return enrollmentRepository.findByStudentId(studentId);
  }

  public List<Enrollment> findByCourseId(String courseId) {
    return enrollmentRepository.findByCourseId(courseId);
  }
}
