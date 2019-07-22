package com.unistudents.api.service.impl;

import com.unistudents.api.model.GradeResults;
import com.unistudents.api.parser.StudentsParser;
import com.unistudents.api.scraper.StudentsScraper;
import com.unistudents.api.service.GradesService;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class GradesServiceImpl implements GradesService {

    @Override
    public GradeResults getGrades(String username, String password) {

        // scrap page
        StudentsScraper scraper = new StudentsScraper(username, password);
        Document gradesPage = scraper.getHtml();

        // parse html to objects
        StudentsParser parser = new StudentsParser(gradesPage);
        GradeResults results = parser.getResults();

        // return objects
        return results;
    }
}
