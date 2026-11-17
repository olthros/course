package com.zjgsu.course.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Instructor {

  @Column(name = "instructor_id")
  private String id;

  @Column(name = "instructor_name")
  private String name;

  @Column(name = "instructor_email")
  private String email;

  protected Instructor() {}

  public Instructor(String id, String name, String email) {
    this.id = id;
    this.name = name;
    this.email = email;
  }

  public String id() { return id; }
  public String name() { return name; }
  public String email() { return email; }
}
