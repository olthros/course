package com.zjgsu.course.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.zjgsu.course.model.Enrollment;
import com.zjgsu.course.repository.EnrollmentRepository;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;

@SpringBootTest
class EnrollmentServiceTest {
  @Autowired EnrollmentService enrollments;
  @Autowired EnrollmentRepository repository;
  @MockitoBean StudentClient students;
  @MockitoBean CatalogClient catalog;

  @BeforeEach
  void prepare() {
    repository.deleteAll();
    when(students.findById("S1")).thenReturn(Map.of("id", "S1"));
  }

  @Test
  void enrollsAndRejectsDuplicateBeforeAnotherRemoteWrite() {
    Enrollment record = enrollments.enroll("S1", "C1");
    assertTrue(repository.existsById(record.getId()));
    assertThrows(IllegalStateException.class, () -> enrollments.enroll("S1", "C1"));
    verify(catalog, times(1)).occupy("C1");
    assertEquals(1, repository.count());
  }

  @Test
  void missingStudentDoesNotOccupyASeat() {
    when(students.findById("S1")).thenThrow(new ResourceAccessException("unavailable"));
    assertThrows(ResourceAccessException.class, () -> enrollments.enroll("S1", "C1"));
    verifyNoInteractions(catalog);
    assertEquals(0, repository.count());
  }

  @Test
  void fullCourseDoesNotCreateEnrollment() {
    doThrow(new IllegalStateException("full")).when(catalog).occupy("C1");
    assertThrows(IllegalStateException.class, () -> enrollments.enroll("S1", "C1"));
    assertEquals(0, repository.count());
  }

  @Test
  void withdrawsByReleasingRemoteSeatThenRemovingLocalRecord() {
    Enrollment record = repository.save(new Enrollment("S1", "C1"));
    enrollments.withdraw(record.getId());
    verify(catalog).release("C1");
    assertFalse(repository.existsById(record.getId()));
  }

  @Test
  void remoteSeatCanBeOccupiedEvenWhenTheCallerTimesOut() {
    AtomicInteger occupied = new AtomicInteger();
    doAnswer(invocation -> {
      occupied.incrementAndGet();
      throw new ResourceAccessException("Response lost after seat was occupied");
    }).when(catalog).occupy("C1");
    assertThrows(ResourceAccessException.class, () -> enrollments.enroll("S1", "C1"));
    assertEquals(0, repository.count());
    assertEquals(1, occupied.get());
  }

  @Test
  void queryingLocalRecordsDoesNotCallOtherServices() {
    repository.save(new Enrollment("S1", "C1"));
    assertEquals(1, enrollments.findByStudentId("S1").size());
    verifyNoInteractions(catalog);
  }

  @Test
  void unavailableCatalogDoesNotDeleteLocalEnrollment() {
    Enrollment record = repository.save(new Enrollment("S1", "C1"));
    doThrow(new ResourceAccessException("unavailable")).when(catalog).release("C1");
    assertThrows(ResourceAccessException.class, () -> enrollments.withdraw(record.getId()));
    assertTrue(repository.existsById(record.getId()));
  }
}
