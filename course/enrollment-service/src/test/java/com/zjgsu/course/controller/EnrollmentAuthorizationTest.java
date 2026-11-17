package com.zjgsu.course.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.zjgsu.course.model.Enrollment;
import com.zjgsu.course.repository.EnrollmentRepository;
import com.zjgsu.course.service.CatalogClient;
import com.zjgsu.course.service.EnrollmentService;
import com.zjgsu.course.service.StudentClient;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class EnrollmentAuthorizationTest {
  @Test
  void cannotReadAnotherStudentsEnrollments() {
    var service = mock(EnrollmentService.class);
    var controller = new EnrollmentController(service);
    assertThrows(ResponseStatusException.class, () -> controller.findByStudent("other", "mine", "STUDENT"));
    verifyNoInteractions(service);
  }

  @Test
  void cannotWithdrawAnotherStudentsEnrollment() {
    var repository = mock(EnrollmentRepository.class);
    var catalog = mock(CatalogClient.class);
    var enrollment = new Enrollment("other", "course");
    when(repository.findById(enrollment.getId())).thenReturn(Optional.of(enrollment));
    var service = new EnrollmentService(repository, mock(StudentClient.class), catalog);
    assertThrows(ResponseStatusException.class,
        () -> service.withdrawAuthorized(enrollment.getId(), "mine", false));
    verifyNoInteractions(catalog);
  }
}
