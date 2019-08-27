package com.unistudents.api.service;

import com.unistudents.api.model.Student;

public interface StudentService {

    public Student getStudent(String username, String password);
}
