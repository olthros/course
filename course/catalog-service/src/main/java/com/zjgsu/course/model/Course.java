package com.zjgsu.course.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "courses", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Course {

  @Id
  private String id;

  @Column(nullable = false)
  private String code;

  @Column(nullable = false)
  private String title;

  @Embedded
  private Instructor instructor;

  @Embedded
  private ScheduleSlot schedule;
  private int capacity;
  private int enrolled;

  protected Course() {}

  public Course(
      String code,
      String title,
      Instructor instructor,
      ScheduleSlot schedule,
      int capacity) {
    if (capacity < 1) {
      throw new IllegalArgumentException("课程容量必须大于 0");
    }
    this.id = UUID.randomUUID().toString();
    this.code = code;
    this.title = title;
    this.instructor = instructor;
    this.schedule = schedule;
    this.capacity = capacity;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public String getTitle() {
    return title;
  }

  public Instructor getInstructor() {
    return instructor;
  }

  public ScheduleSlot getSchedule() {
    return schedule;
  }

  public int getCapacity() {
    return capacity;
  }

  public int getEnrolled() {
    return enrolled;
  }

  public void update(String code, String title, Instructor instructor,
      ScheduleSlot schedule, int capacity) {
    if (capacity < 1 || capacity < enrolled) {
      throw new IllegalArgumentException("课程容量不能小于已选人数或 1");
    }
    this.code = code;
    this.title = title;
    this.instructor = instructor;
    this.schedule = schedule;
    this.capacity = capacity;
  }

  public boolean isFull() {
    return enrolled >= capacity;
  }

  public void enrollStudent() {
    if (isFull()) {
      throw new IllegalStateException("课程人数已满");
    }
    enrolled++;
  }

  public void withdrawStudent() {
    if (enrolled > 0) {
      enrolled--;
    }
  }
}
