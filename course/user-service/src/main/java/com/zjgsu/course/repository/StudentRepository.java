package com.zjgsu.course.repository;

import com.zjgsu.course.model.Student;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, String> {

  Optional<Student> findByStudentNumber(String studentNumber);
}
