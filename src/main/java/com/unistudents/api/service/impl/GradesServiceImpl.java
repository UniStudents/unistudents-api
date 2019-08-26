package com.unistudents.api.service.impl;

import com.unistudents.api.model.Grades;
import com.unistudents.api.service.GradesService;
import org.springframework.stereotype.Service;

@Service
public class GradesServiceImpl implements GradesService {

    @Override
    public Grades getGrades(String username, String password) {

//        // scrap grades page
//        StudentsScraper scraper = new StudentsScraper(username, password);
//        // authorized check
//        if (!scraper.isAuthorized()) {
//            return null;
//        }
//
//        Document gradesPage = scraper.getGradesPage();
//
//        // parse grades page to object
//        StudentsParser parser = new StudentsParser(studentInfoPage, gradesPage);
//        GradeResults results = parser.getResults();
//
//        // return object
//        return results;
        return null;
    }

}
