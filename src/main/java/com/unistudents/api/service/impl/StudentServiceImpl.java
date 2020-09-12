package com.unistudents.api.service.impl;

import com.unistudents.api.model.Student;
import com.unistudents.api.parser.UNIPIParser;
import com.unistudents.api.scraper.UNIPIScraper;
import com.unistudents.api.service.StudentService;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class StudentServiceImpl implements StudentService {

    @Override
    public Student getStudent(String username, String password) {

        // scrap info page
        UNIPIScraper scraper = new UNIPIScraper(username, password);

        // authorized check
        if (!scraper.isAuthorized()) {
            return null;
        }

        Document infoPage = scraper.getStudentInfoPage();
        Document gradesPage = scraper.getGradesPage();

        UNIPIParser parser = new UNIPIParser();
        return parser.parseInfoAndGradesPages(infoPage, gradesPage);
    }
}
