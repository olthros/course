package com.zjgsu.course.service;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CatalogFallbackFactory implements FallbackFactory<CatalogClient> {
  @Override
  public CatalogClient create(Throwable cause) {
    return new CatalogClient() {
      @Override
      public void occupy(String id) {
        throw failure(cause);
      }

      @Override
      public void release(String id) {
        throw failure(cause);
      }
    };
  }

  private ResponseStatusException failure(Throwable cause) {
    if (cause instanceof FeignException ex
        && (ex.status() == 400 || ex.status() == 404 || ex.status() == 409)) {
      return new ResponseStatusException(HttpStatusCode.valueOf(ex.status()),
          "Catalog service rejected the request");
    }
    return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
        "Catalog service unavailable; a timed-out write may already have completed");
  }
}
