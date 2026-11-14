package com.zjgsu.course.service;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class StudentFallbackFactory implements FallbackFactory<StudentClient> {
  @Override
  public StudentClient create(Throwable cause) {
    return id -> {
      if (cause instanceof FeignException ex
          && (ex.status() == 400 || ex.status() == 404 || ex.status() == 409)) {
        throw new ResponseStatusException(HttpStatusCode.valueOf(ex.status()),
            "Student service rejected the request");
      }
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Student service unavailable");
    };
  }
}
