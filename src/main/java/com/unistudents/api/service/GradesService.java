package com.unistudents.api.service;

import com.unistudents.api.model.GradeResults;

public interface GradesService {

    public GradeResults getGrades(String username, String password);
}
