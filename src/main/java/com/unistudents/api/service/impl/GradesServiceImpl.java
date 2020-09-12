package com.unistudents.api.service.impl;

import com.unistudents.api.model.Grades;
import com.unistudents.api.parser.UNIPIParser;
import com.unistudents.api.scraper.UNIPIScraper;
import com.unistudents.api.service.GradesService;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class GradesServiceImpl implements GradesService {

    @Override
    public Grades getGrades(String username, String password) {

        // scrap grades page
        UNIPIScraper scraper = new UNIPIScraper(username, password);

        // authorized check
        if (!scraper.isAuthorized()) {
            return null;
        }

        Document gradesPage = scraper.getGradesPage();

        // return object
        UNIPIParser parser = new UNIPIParser();
        return parser.parseGradesPage(gradesPage);
    }

}
