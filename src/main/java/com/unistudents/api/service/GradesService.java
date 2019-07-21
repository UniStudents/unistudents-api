package com.unistudents.api.service;

import com.unistudents.api.model.GradeResults;

public interface GradesService {

    public GradeResults getGrades(String username, String password);
    public String getTest(String username, String password);
}
