package com.unistudents.api.service.impl;

import com.unistudents.api.model.Grades;
import com.unistudents.api.parser.StudentsParser;
import com.unistudents.api.scraper.StudentsScraper;
import com.unistudents.api.service.GradesService;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class GradesServiceImpl implements GradesService {

    @Override
    public Grades getGrades(String username, String password) {

        // scrap grades page
        StudentsScraper scraper = new StudentsScraper(username, password);

        // authorized check
        if (!scraper.isAuthorized()) {
            return null;
        }

        Document gradesPage = scraper.getGradesPage();

        // return object
        StudentsParser parser = new StudentsParser();
        return parser.parseGradesPage(gradesPage);
    }

}
