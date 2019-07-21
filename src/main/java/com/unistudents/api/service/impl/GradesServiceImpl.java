package com.unistudents.api.service.impl;

import com.unistudents.api.model.GradeResults;
import com.unistudents.api.scraper.StudentsScraper;
import com.unistudents.api.service.GradesService;
import org.springframework.stereotype.Service;

@Service
public class GradesServiceImpl implements GradesService {

    @Override
    public GradeResults getGrades(String username, String password) {
        return null;
    }

    public String getTest(String username, String password) {

        StudentsScraper scraper = new StudentsScraper(username, password);

        return scraper.getHtml();
    }
}
