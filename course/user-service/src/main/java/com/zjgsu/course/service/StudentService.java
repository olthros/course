package com.zjgsu.course.service;

import com.zjgsu.course.exception.ResourceNotFoundException;
import com.zjgsu.course.model.Student;
import com.zjgsu.course.repository.StudentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudentService {

  private final StudentRepository studentRepository;

  public StudentService(StudentRepository studentRepository) {
    this.studentRepository = studentRepository;
  }

  public Student createStudent(Student student) {
    if (studentRepository.findByStudentNumber(student.getStudentNumber()).isPresent()) {
      throw new IllegalStateException("学号已存在: " + student.getStudentNumber());
    }
    return studentRepository.save(student);
  }

  public Student findById(String id) {
    return studentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("学生不存在: " + id));
  }

  public List<Student> findAll() {
    return studentRepository.findAll();
  }

  public Student updateStudent(String id, Student changes) {
    Student student = findById(id);
    studentRepository.findByStudentNumber(changes.getStudentNumber()).ifPresent(existing -> {
      if (!existing.getId().equals(id)) {
        throw new IllegalStateException("学号已存在: " + changes.getStudentNumber());
      }
    });
    student.update(changes.getStudentNumber(), changes.getName(), changes.getMajor());
    return studentRepository.save(student);
  }
}
