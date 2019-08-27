package com.unistudents.api.service.impl;

import com.unistudents.api.model.Info;
import com.unistudents.api.parser.StudentsParser;
import com.unistudents.api.scraper.StudentsScraper;
import com.unistudents.api.service.InfoService;
import org.springframework.stereotype.Service;
import org.jsoup.nodes.Document;

@Service
public class InfoServiceImpl implements InfoService {

    @Override
    public Info getInfo(String username, String password) {

        // scrap info page
        StudentsScraper scraper = new StudentsScraper(username, password);

        // authorized check
        if (!scraper.isAuthorized()) {
            return null;
        }

        Document infoPage = scraper.getStudentInfoPage();

        // return object
        StudentsParser parser = new StudentsParser();
        return parser.parseInfoPage(infoPage);
    }
}
