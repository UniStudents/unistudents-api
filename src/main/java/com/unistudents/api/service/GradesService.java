package com.unistudents.api.service;

import com.unistudents.api.model.Grades;

public interface GradesService {

    public Grades getGrades(String username, String password);
}
