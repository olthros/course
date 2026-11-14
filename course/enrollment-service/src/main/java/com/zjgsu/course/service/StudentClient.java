package com.zjgsu.course.service;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", fallbackFactory = StudentFallbackFactory.class)
public interface StudentClient {
  @GetMapping("/api/students/{id}")
  Map<String, Object> findById(@PathVariable("id") String id);
}
