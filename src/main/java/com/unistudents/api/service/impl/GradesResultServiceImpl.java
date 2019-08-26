package com.unistudents.api.service.impl;

import com.unistudents.api.model.GradeResults;
import com.unistudents.api.parser.StudentsParser;
import com.unistudents.api.scraper.StudentsScraper;
import com.unistudents.api.service.GradesResultService;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class GradesResultServiceImpl implements GradesResultService {

    @Override
    public GradeResults getGrades(String username, String password) {

        // scrap page
        StudentsScraper scraper = new StudentsScraper(username, password);

        if (!scraper.isAuthorized()) {
            return null;
        }

        Document studentInfoPage = scraper.getStudentInfoPage();
        Document gradesPage = scraper.getGradesPage();

        // parse html to objects
        StudentsParser parser = new StudentsParser(studentInfoPage, gradesPage);
        GradeResults results = parser.getResults();

        // return objects
        return results;
    }
}
