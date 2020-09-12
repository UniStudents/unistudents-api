package com.unistudents.api.service.impl;

import com.unistudents.api.model.Info;
import com.unistudents.api.parser.UNIPIParser;
import com.unistudents.api.scraper.UNIPIScraper;
import com.unistudents.api.service.InfoService;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class InfoServiceImpl implements InfoService {

    @Override
    public Info getInfo(String username, String password) {

        // scrap info page
        UNIPIScraper scraper = new UNIPIScraper(username, password);

        // authorized check
        if (!scraper.isAuthorized()) {
            return null;
        }

        Document infoPage = scraper.getStudentInfoPage();

        // return object
        UNIPIParser parser = new UNIPIParser();
        return parser.parseInfoPage(infoPage);
    }
}
