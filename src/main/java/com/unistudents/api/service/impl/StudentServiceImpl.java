package com.unistudents.api.service.impl;

import com.unistudents.api.model.Student;
import com.unistudents.api.parser.StudentsParser;
import com.unistudents.api.scraper.StudentsScraper;
import com.unistudents.api.service.StudentService;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class StudentServiceImpl implements StudentService {

    @Override
    public Student getStudent(String username, String password) {

        // scrap info page
        StudentsScraper scraper = new StudentsScraper(username, password);

        // authorized check
        if (!scraper.isAuthorized()) {
            return null;
        }

        Document infoPage = scraper.getStudentInfoPage();
        Document gradesPage = scraper.getGradesPage();

        StudentsParser parser = new StudentsParser();
        return parser.parseInfoAndGradesPages(infoPage, gradesPage);
    }
}
