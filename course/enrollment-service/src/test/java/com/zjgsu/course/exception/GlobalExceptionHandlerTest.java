package com.zjgsu.course.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import feign.FeignException;
import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GlobalExceptionHandlerTest {
  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  private FeignException error(int status) {
    Request request = Request.create(Request.HttpMethod.GET, "http://example/test",
        Map.of(), null, StandardCharsets.UTF_8, null);
    return FeignException.errorStatus("test", Response.builder()
        .request(request).status(status).reason("test").build());
  }

  @Test
  void preservesBusinessRejections() {
    assertEquals(404, handler.feignError(error(404)).getStatus());
    assertEquals(409, handler.feignError(error(409)).getStatus());
  }

  @Test
  void mapsRemoteFailuresToUnavailable() {
    assertEquals(503, handler.feignError(error(500)).getStatus());
    assertEquals(503, handler.feignError(error(503)).getStatus());
  }
}
