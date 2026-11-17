package com.zjgsu.course.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Embeddable
public class ScheduleSlot {

  @Enumerated(EnumType.STRING)
  @Column(name = "day_of_week")
  private DayOfWeek dayOfWeek;

  @Column(name = "start_time")
  private LocalTime startTime;

  @Column(name = "end_time")
  private LocalTime endTime;

  protected ScheduleSlot() {}

  public ScheduleSlot(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    this.dayOfWeek = dayOfWeek;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public DayOfWeek dayOfWeek() { return dayOfWeek; }
  public LocalTime startTime() { return startTime; }
  public LocalTime endTime() { return endTime; }
}
