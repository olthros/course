package com.zjgsu.course.exception;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(FeignException.class)
  ProblemDetail feignError(FeignException ex) {
    int status = ex.status();
    if (status >= 400 && status < 500) {
      return ProblemDetail.forStatusAndDetail(
          org.springframework.http.HttpStatusCode.valueOf(status),
          "Dependent service rejected the request");
    }
    return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
        "Dependent service unavailable; a timed-out write may already have completed");
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  ProblemDetail notFound(ResourceNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler({IllegalStateException.class, DataIntegrityViolationException.class})
  ProblemDetail conflict(Exception ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
  ProblemDetail badRequest(Exception ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(HttpClientErrorException.class)
  ProblemDetail remoteClientError(HttpClientErrorException ex) {
    return ProblemDetail.forStatusAndDetail(ex.getStatusCode(),
        "Dependent service rejected the request: " + ex.getStatusText());
  }

  @ExceptionHandler(RestClientException.class)
  ProblemDetail unavailable(RestClientException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
        "Dependent service unavailable; a timed-out write may already have completed");
  }
}
