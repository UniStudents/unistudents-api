package com.unistudents.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unistudents.api.common.Services;
import com.unistudents.api.model.LoginForm;
import com.unistudents.api.model.Student;
import com.unistudents.api.model.StudentDTO;
import com.unistudents.api.parser.*;
import com.unistudents.api.scraper.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class ScrapeService {

    private ExecutorService executor = Executors.newCachedThreadPool();

    public ResponseEntity getLogin(String university) {
        switch (university) {
            case "UOP":
                return new TEIWESTScraper(university).getLoginPage();
            case "UPATRAS":
                return new TEIWESTScraper(university).getLoginPage();
            default:
                return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    public ResponseEntity getStudent(String university, String system, LoginForm loginForm) {
        if (isGuest(loginForm))
            return getGuestStudent();

        if (system == null)
            return getStudent(university, loginForm);

        switch (university) {
            case "AEGEAN":
                switch (system) {
                    case "CARDISOFT":
                    case "UNIVERSIS":
                        return getAEGEANSISStudent(loginForm);
                    case "SEF":
                        return getSEFStudent(loginForm);
                    case "ICARUS":
                        return getICARUSStudent(loginForm);
                    default:
                        return new ResponseEntity(HttpStatus.NOT_FOUND);
                }
            case "IHU":
                switch (system) {
                    case "TEITHE":
                        return getCardisoftStudent(loginForm, university, system, "pithia.teithe.gr", "/unistudent", false);
                    case "CM":
                        return getCardisoftStudent(loginForm, university, system, "egram.cm.ihu.gr", "/unistudent", true);
                    case "TEIEMT":
                        return getCardisoftStudent(loginForm, university, system, "e-secretariat.teiemt.gr", "/unistudent", true);
                    default:
                        return new ResponseEntity(HttpStatus.NOT_FOUND);
                }
            case "AUA":
                switch (system) {
                    case "ILYDA":
                        return getILYDAStudent(loginForm, university, system, "unistudent.aua.gr");
                    case "CUSTOM":
                        return getAUACustomStudent(loginForm);
                    default:
                        return new ResponseEntity(HttpStatus.NOT_FOUND);
                }
            case "UOP":
                switch (system) {
                    case "MAIN":
                        return getCardisoftStudent(loginForm, university, system, "e-secretary.uop.gr", "/UniStudent", true);
                    case "TEIPEL":
                        return getCardisoftStudent(loginForm, university, system, "www.webgram.teikal.gr", "/unistudent", false);
                    case "TEIWEST":
                        return getTEIWESTStudent(loginForm, university, system);
                    default:
                        return new ResponseEntity(HttpStatus.NOT_FOUND);
                }
            case "UPATRAS":
                switch (system) {
                    case "PROGRESS":
                        return getPROGRESSStudent(loginForm);
                    case "TEIWEST":
                        return getTEIWESTStudent(loginForm, university, system);
                    default:
                        return new ResponseEntity(HttpStatus.NOT_FOUND);
                }
            default:
                return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    private ResponseEntity getStudent(String university, LoginForm loginForm) {
        switch (university) {
            case "UOA":
                return getUOAStudent(loginForm);
            case "PANTEION":
                return getPANTEIONStudent(loginForm);
            case "UPATRAS":
                return getUPATRASStudent(loginForm);
            case "AUEB":
                return getAUEBStudent(loginForm);
            case "HUA":
                return getHUAStudent(loginForm);
            case "NTUA":
                return getNTUAStudent(loginForm);
            case "IHU":
                return getIHUStudent(loginForm);
            case "AEGEAN":
                return getAEGEANStudent(loginForm);
            case "AUA":
                return getAUAStudent(loginForm);
            case "UOP":
                return getUOPStudent(loginForm);
            case "UOI":
                return getILYDAStudent(loginForm, university, null, "classweb.uoi.gr");
            case "UNIWA":
                return getILYDAStudent(loginForm, university, null, "services.uniwa.gr");
            case "UNIPI":
                return getCardisoftStudent(loginForm, university, null, "students.unipi.gr", "", true);
            case "UOC":
                return getUOCStudent(loginForm, university, null, "eduportal.cict.uoc.gr");
            case "TUC":
                return getTUCStudent(loginForm);
            case "UOWM":
                return getCardisoftStudent(loginForm, university, null, "students.uowm.gr", "", true);
            case "HMU":
                return getHMUStudent(loginForm);
            case "IONIO":
                return getILYDAStudent(loginForm, university, null, "dias.ionio.gr");
            case "ASPETE":
                return getCardisoftStudent(loginForm, university, null, "studentweb.aspete.gr", "", true);
            case "AUTH":
                return getAUTHStudent(loginForm);
            case "DUTH":
                return getDUTHStudent(loginForm);
            case "GUEST":
                return getGuestStudent();
            default:
                return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    /*
     *
     *
     * SCRAPE SERVICES FOR UNIS
     *
     *
     */

    private ResponseEntity getCardisoftStudent(LoginForm loginForm, String university, String system, String domain, String pathURL, boolean SSL) {
        // scrap info page
        CardisoftScraper scraper = new CardisoftScraper(loginForm, university, system, domain, pathURL, SSL);

        // check for connection errors
        if (!scraper.isConnected()) {
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        }

        // authorized check
        if (!scraper.isAuthorized()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Document infoPage = scraper.getStudentInfoPage();
        Document gradesPage = scraper.getGradesPage();

        // check for errors
        if (infoPage == null || gradesPage == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        CardisoftParser parser = new CardisoftParser(university, system);
        Student student = parser.parseInfoAndGradesPages(infoPage, gradesPage);

        if (student == null) {
            return new ResponseEntity(new Services().uploadLogFile(parser.getException(), parser.getDocument(), university.toUpperCase()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        StudentDTO studentDTO = new StudentDTO(system, scraper.getCookies(), student);

        return new ResponseEntity<>(studentDTO, HttpStatus.OK);
    }

    private ResponseEntity getILYDAStudent(LoginForm loginForm, String university, String system, String domain) {
        // scrap info page
        ILYDAScraper scraper = new ILYDAScraper(loginForm, university, system, domain);

        // check for connection errors
        if (!scraper.isConnected()) {
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        }

        // authorization check
        if (!scraper.isAuthorized()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String infoJSON = scraper.getInfoJSON();
        String gradesJSON = scraper.getGradesJSON();
        String totalAverageGrade = scraper.getTotalAverageGrade();

        // check for internal errors
        if (infoJSON == null || gradesJSON == null || totalAverageGrade == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ILYDAParser parser = new ILYDAParser(university, system);
        Student student = parser.parseInfoAndGradesJSON(infoJSON, gradesJSON, totalAverageGrade);

        if (student == null) {
            return new ResponseEntity(new Services().uploadLogFile(parser.getException(), parser.getDocument(), university.toUpperCase()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        StudentDTO studentDTO = new StudentDTO(system, scraper.getCookies(), student);

        return new ResponseEntity<>(studentDTO, HttpStatus.OK);
    }

    private ResponseEntity getUOCStudent(LoginForm loginForm, String university, String system, String domain) {
        UOCScraper scraper = new UOCScraper(loginForm, university, system, domain);

        // check for connection errors
        if (!scraper.isConnected()) {
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        }

        // authorization check
        if (!scraper.isAuthorized()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String infoJSON = scraper.getInfoJSON();
        String gradesJSON = scraper.getGradesJSON();
        String totalAverageGrade = scraper.getTotalAverageGrade();

        // check for internal errors
        if (infoJSON == null || gradesJSON == null || totalAverageGrade == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ILYDAParser parser = new ILYDAParser(university, system);
        Student student = parser.parseInfoAndGradesJSON(infoJSON, gradesJSON, totalAverageGrade);

        if (student == null) {
            return new ResponseEntity(new Services().uploadLogFile(parser.getException(), parser.getDocument(), university.toUpperCase()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        StudentDTO studentDTO = new StudentDTO(system, scraper.getCookies(), student);

        return new ResponseEntity<>(studentDTO, HttpStatus.OK);
    }

    private ResponseEntity getUOAStudent(LoginForm loginForm) {
        // scrape student information
        UNIWAYScraper scraper = new UNIWAYScraper(loginForm);

        // check for connection errors
        if (!scraper.isConnected()) {
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        }

        // authorization check
        if (!scraper.isAuthorized()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String infoJSON = scraper.getStudentInfoJSON();
        String gradesJSON = scraper.getGradesJSON();
        String declareHistoryJSON = scraper.getDeclareHistoryJSON();

        // check for internal errors
        if (infoJSON == null || gradesJSON == null || declareHistoryJSON == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        UNIWAYParser parser = new UNIWAYParser();
        Student student = parser.parseInfoAndGradesPages(infoJSON, gradesJSON, declareHistoryJSON);

        if (student == null) {
            return new ResponseEntity(new Services().uploadLogFile(parser.getException(), parser.getDocument(), "UOA"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        student.getInfo().setSemester(String.valueOf(student.getGrades().getSemesters().size()));

        StudentDTO studentDTO = new StudentDTO(scraper.getCookies(), student);

        return new ResponseEntity<>(studentDTO, HttpStatus.OK);
    }

    private ResponseEntity getPANTEIONStudent(LoginForm loginForm) {

        try {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("https://panteion.unistudents.gr/api/student/panteion");

            String json = "{\n    \"username\": \"" + loginForm.getUsername() + "\",\n    \"password\": \"" + loginForm.getPassword() + "\",\n    \"cookies\": null\n}";
            StringEntity entity = new StringEntity(json);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");

            CloseableHttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                StudentDTO studentDTO = new ObjectMapper().readValue(response.getEntity().getContent(), StudentDTO.class);
                return new ResponseEntity<>(studentDTO, HttpStatus.OK);
            } else {
                return new ResponseEntity(HttpStatus.valueOf(response.getStatusLine().getStatusCode()));
            }
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity getPROGRESSStudent(LoginForm loginForm) {
        UPATRASScraper scraper = new UPATRASScraper(loginForm);

        // check for connection errors
        if (!scraper.isConnected()) {
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        }

        // authorization check
        if (!scraper.isAuthorized()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Document infoAndGradesPage = scraper.getInfoAndGradesPage();

        // check for internal errors
        if (infoAndGradesPage == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        UPATRASParser parser = new UPATRASParser();
        Student student = parser.parseInfoAndGradesPage(infoAndGradesPage);

        if (student == null) {
            return new ResponseEntity<>(new Services().uploadLogFile(parser.getException(), parser.getDocument(), "UPATRAS"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        StudentDTO studentDTO = new StudentDTO("PROGRESS", scraper.getCookies(), student);

        return new ResponseEntity<>(studentDTO, HttpStatus.OK);
    }

    private ResponseEntity getAUEBStudent(LoginForm loginForm) {
        AUEBScraper scraper = new AUEBScraper(loginForm);

        // check for connection errors
        if (!scraper.isConnected()) {
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        }

        // authorization check
        if (!scraper.isAuthorized()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Document infoAndGradesPage = scraper.getStudentInfoAndGradesPage();

        // check for internal errors
        if (infoAndGradesPage == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ARCHIMEDIAParser parser = new ARCHIMEDIAParser();
        Student student = parser.parseInfoAndGradesPages(infoAndGradesPage);

        if (student == null) {
            return new ResponseEntity(new Services().uploadLogFile(parser.getException(), parser.getDocument(), "AUEB"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        StudentDTO studentDTO = new StudentDTO(scraper.getCookies(), student);

        return new ResponseEntity<>(studentDTO, HttpStatus.OK);
    }

    private ResponseEntity getHUAStudent(LoginForm loginForm) {
        HUAScraper scraper = new HUAScraper(loginForm);

        // check for connection errors
        if (!scraper.isConnected()) {
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        }

        // authorization check
        if (!scraper.isAuthorized()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Document infoAndGradesPage = scraper.getStudentInfoAndGradesPage();

        // check for internal errors
        if (infoAndGradesPage == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ARCHIMEDIAParser parser = new ARCHIMEDIAParser();
        Student student = parser.parseInfoAndGradesPages(infoAndGradesPage);

        if (student == null) {
            return new ResponseEntity(new Services().uploadLogFile(parser.getException(), parser.getDocument(), "HUA"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        StudentDTO studentDTO = new StudentDTO(scraper.getCookies(), student);

        return new ResponseEntity<>(studentDTO, HttpStatus.OK);
    }

    private ResponseEntity getAUACustomStudent(LoginForm loginForm) {
        AUAScraper scraper = new AUAScraper(loginForm);

        // check for connection errors
        if (!scraper.isConnected()) {
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        }

        // authorization check
        if (!scraper.isAuthorized()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Document infoPage = scraper.getStudentInfoPage();
        Document gradesPage = scraper.getGradesPage();

        // check for internal errors
        if (infoPage == null || gradesPage == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        AUAParser parser = new AUAParser();
        Student student = parser.parseInfoAndGradesPages(infoPage, gradesPage);

        if (student == null) {
            return new ResponseEntity(new Services().uploadLogFile(parser.getException(), parser.getDocument(), "AUA"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        StudentDTO studentDTO = new StudentDTO("CUSTOM", scraper.getCookies(), student);

        return new ResponseEntity<>(studentDTO, HttpStatus.OK);
    }

    private ResponseEntity getICARUSStudent(LoginForm loginForm) {
        ICARUSScraper scraper = new ICARUSScraper(loginForm);

        // check for connection errors
        if (!scraper.isConnected()) {
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        }

        // authorization check
        if (!scraper.isAuthorized()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Document infoAndGradePage = scraper.getInfoAndGradePage();

        if (infoAndGradePage == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ICARUSParser parser = new ICARUSParser();
        Student student = parser.parseInfoAndGradesPages(infoAndGradePage);

        if (student == null) {
            return new ResponseEntity(new Services().uploadLogFile(parser.getException(), parser.getDocument(), "ICARUS"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        StudentDTO studentDTO = new StudentDTO("ICARUS", scraper.getCookies(), student);

        return new ResponseEntity<>(studentDTO, HttpStatus.OK);
    }

    private ResponseEntity getSEFStudent(LoginForm loginForm) {
        SEFScraper scraper = new SEFScraper(loginForm);

        // check for connection errors
        if (!scraper.isConnected()) {
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        }

        // authorization check
        if (!scraper.isAuthorized()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Document infoPage = scraper.getStudentInfoPage();
        Document gradesPage = scraper.getGradesPage();

        if (infoPage == null || gradesPage == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        SEFParser parser = new SEFParser();
        Student student = parser.parseInfoAndGradesPages(infoPage, gradesPage);

        if (student == null) {
            return new ResponseEntity<>(new Services().uploadLogFile(parser.getException(), parser.getDocument(), "AEGEAN-SEF"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        StudentDTO studentDTO = new StudentDTO("SEF", scraper.getCookies(), student);

        return new ResponseEntity<>(studentDTO, HttpStatus.OK);
    }

    private ResponseEntity getNTUAStudent(LoginForm loginForm) {
        NTUAScraper scraper = new NTUAScraper(loginForm);
        Map<String, String> cookies;

        // check for connection errors
        if (!scraper.isConnected()) {
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        }

        // authorization check
        if (!scraper.isAuthorized()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String infoAndGradesJSON = scraper.getStudentInfoAndGradesPage();

        if (infoAndGradesJSON == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ECEScraper eceScraper = null;
        Document infoAndGradePage = null;
        if (loginForm.getUsername().startsWith("el") || scraper.getCookies().get("department").equals("3")) {
            eceScraper = new ECEScraper(loginForm);
            infoAndGradePage = eceScraper.getStudentInfoAndGradesPage();
            if (infoAndGradePage == null) {
                return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        NTUAParser parser = new NTUAParser();
        Student student = parser.parseJSONAndDocument(infoAndGradesJSON, infoAndGradePage);

        if (student == null) {
            return new ResponseEntity(new Services().uploadLogFile(parser.getException(), parser.getDocument(), "NTUA"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        cookies = scraper.getCookies();
        if (eceScraper != null)
            cookies.putAll(eceScraper.getCookies());
        StudentDTO studentDTO = new StudentDTO(null, cookies, student);

        return new ResponseEntity<>(studentDTO, HttpStatus.OK);
    }

    private ResponseEntity getTUCStudent(LoginForm loginForm) {
        TUCScraper scraper = new TUCScraper(loginForm);

        // check for connection errors
        if (!scraper.isConnected()) {
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        }

        // authorization check
        if (!scraper.isAuthorized()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String infoJSON = scraper.getInfoJSON();
        String gradesJSON = scraper.getGradesJSON();

        if (infoJSON == null || gradesJSON == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        UNIVERSISParser parser = new UNIVERSISParser("TUC");
        Student student = parser.parseInfoAndGradesJSON(infoJSON, gradesJSON);

        if (student == null) {
            return new ResponseEntity(new Services().uploadLogFile(parser.getException(), parser.getDocument(), "TUC"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        StudentDTO studentDTO = new StudentDTO(null, scraper.getCookies(), student);

        return new ResponseEntity<>(studentDTO, HttpStatus.OK);
    }

    private ResponseEntity getHMUStudent(LoginForm loginForm) {
        HMUScraper scraper = new HMUScraper(loginForm);

        // check for connection errors
        if (!scraper.isConnected()) {
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        }

        // authorization check
        if (!scraper.isAuthorized()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String infoJSON = scraper.getInfoJSON();
        String gradesJSON = scraper.getGradesJSON();

        if (infoJSON == null || gradesJSON == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        UNIVERSISParser parser = new UNIVERSISParser("HMU");
        Student student = parser.parseInfoAndGradesJSON(infoJSON, gradesJSON);

        if (student == null) {
            return new ResponseEntity(new Services().uploadLogFile(parser.getException(), parser.getDocument(), "HMU"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        StudentDTO studentDTO = new StudentDTO(null, scraper.getCookies(), student);

        return new ResponseEntity<>(studentDTO, HttpStatus.OK);
    }

    private ResponseEntity getAUTHStudent(LoginForm loginForm) {
        AUTHScraper scraper = new AUTHScraper(loginForm);

        // check for connection errors
        if (!scraper.isConnected()) {
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        }

        // authorization check
        if (!scraper.isAuthorized()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String infoJSON = scraper.getInfoJSON();
        String gradesJSON = scraper.getGradesJSON();

        if (infoJSON == null || gradesJSON == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        UNIVERSISParser parser = new UNIVERSISParser("AUTH");
        Student student = parser.parseInfoAndGradesJSON(infoJSON, gradesJSON);

        if (student == null) {
            return new ResponseEntity(new Services().uploadLogFile(parser.getException(), parser.getDocument(), "AUTH"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        StudentDTO studentDTO = new StudentDTO(null, scraper.getCookies(), student);

        return new ResponseEntity<>(studentDTO, HttpStatus.OK);
    }

    private ResponseEntity getDUTHStudent(LoginForm loginForm) {
        DUTHScraper scraper = new DUTHScraper(loginForm);

        // check for connection errors
        if (!scraper.isConnected()) {
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        }

        // authorization check
        if (!scraper.isAuthorized()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String infoJSON = scraper.getInfoJSON();
        String gradesJSON = scraper.getGradesJSON();

        if (infoJSON == null || gradesJSON == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        UNIVERSISParser parser = new UNIVERSISParser("DUTH");
        Student student = parser.parseInfoAndGradesJSON(infoJSON, gradesJSON);

        if (student == null) {
            return new ResponseEntity(new Services().uploadLogFile(parser.getException(), parser.getDocument(), "DUTH"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        StudentDTO studentDTO = new StudentDTO(null, scraper.getCookies(), student);

        return new ResponseEntity<>(studentDTO, HttpStatus.OK);
    }

    private ResponseEntity getAEGEANSISStudent(LoginForm loginForm) {
        AEGEANScraper scraper = new AEGEANScraper(loginForm);

        // check for connection errors
        if (!scraper.isConnected()) {
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        }

        // authorization check
        if (!scraper.isAuthorized()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String infoJSON = scraper.getInfoJSON();
        String gradesJSON = scraper.getGradesJSON();

        if (infoJSON == null || gradesJSON == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        UNIVERSISParser parser = new UNIVERSISParser("AEGEAN");
        Student student = parser.parseInfoAndGradesJSON(infoJSON, gradesJSON);

        if (student == null) {
            return new ResponseEntity(new Services().uploadLogFile(parser.getException(), parser.getDocument(), "AEGEAN"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        StudentDTO studentDTO = new StudentDTO("UNIVERSIS", scraper.getCookies(), student);

        return new ResponseEntity<>(studentDTO, HttpStatus.OK);
    }

    private ResponseEntity getTEIWESTStudent(LoginForm loginForm, String university, String system) {
        TEIWESTScraper scraper = new TEIWESTScraper(loginForm, university);

        if (!scraper.isConnected()) {
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        }

        if (scraper.isCaptchaRequired()) {
            return new ResponseEntity(new StudentDTO("TEIWEST", scraper.getCookies(), null), HttpStatus.OK);
        }

        Document infoPage = scraper.getInfoPage();
        Document gradesPage = scraper.getGradesPage();

        if (infoPage == null || gradesPage == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        TEIWESTParser parser = new TEIWESTParser(university);
        Student student = parser.parseInfoAndGradesDocuments(infoPage, gradesPage);

        if (student == null) {
            return new ResponseEntity(new Services().uploadLogFile(parser.getException(), parser.getDocument(), university.toUpperCase() + ".TEIWEST"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        StudentDTO studentDTO = new StudentDTO(system, scraper.getCookies(), student);

        return new ResponseEntity<>(studentDTO, HttpStatus.OK);
    }

    private ResponseEntity getIHUStudent(LoginForm loginForm) {
        List<Future<ResponseEntity>> futures = new ArrayList<>();
        futures.add(executor.submit(() -> {
            try {
                return getCardisoftStudent(loginForm, "IHU", "TEITHE", "pithia.teithe.gr", "/unistudent", false);
            } catch (Exception e) {
                return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }));
        futures.add(executor.submit(() -> {
            try {
                return getCardisoftStudent(loginForm, "IHU", "CM", "egram.cm.ihu.gr", "/unistudent", true);
            } catch (Exception e) {
                return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }));
        futures.add(executor.submit(() -> {
            try {
                return getCardisoftStudent(loginForm, "IHU", "TEIEMT", "e-secretariat.teiemt.gr", "/unistudent", true);
            } catch (Exception e) {
                return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }));

        return getFuturesResults(futures);
    }

    private ResponseEntity getAEGEANStudent(LoginForm loginForm) {
        String username = loginForm.getUsername();
        if (username.contains("icsd")) {
            return getICARUSStudent(loginForm);
        } else if (username.contains("sas") || username.contains("math")) {
            return getSEFStudent(loginForm);
        } else {
            return getAEGEANSISStudent(loginForm);
        }
    }

    private ResponseEntity getAUAStudent(LoginForm loginForm) {
        List<Future<ResponseEntity>> futures = new ArrayList<>();
        futures.add(executor.submit(() -> {
            try {
                return getAUACustomStudent(loginForm);
            } catch (Exception e) {
                return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }));
        futures.add(executor.submit(() -> {
            try {
                return getILYDAStudent(loginForm, "AUA", "ILYDA", "unistudent.aua.gr");
            } catch (Exception e) {
                return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }));

        return getFuturesResults(futures);
    }

    private ResponseEntity getUOPStudent(LoginForm loginForm) {
        List<Future<ResponseEntity>> futures = new ArrayList<>();
        futures.add(executor.submit(() -> {
            try {
                return getCardisoftStudent(loginForm, "UOP", "MAIN", "e-secretary.uop.gr", "/UniStudent", true);
            } catch (Exception e) {
                return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }));
        futures.add(executor.submit(() -> {
            try {
                return getCardisoftStudent(loginForm, "UOP", "TEIPEL", "www.webgram.teikal.gr", "/unistudent", false);
            } catch (Exception e) {
                return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }));

        ResponseEntity responseEntity = getFuturesResults(futures);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return getLogin("UOP");
        }

        return responseEntity;
    }

    private ResponseEntity getUPATRASStudent(LoginForm loginForm) {
        ResponseEntity responseEntity = getPROGRESSStudent(loginForm);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return getLogin("UPATRAS");
        }

        return responseEntity;
    }

    private ResponseEntity getGuestStudent() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = null;

        try {
            byte[] encoded = Files.readAllBytes(Paths.get("src/main/resources/guestStudent.json"));
            String jsonFile = new String(encoded, StandardCharsets.UTF_8);

            json = mapper.readTree(jsonFile);
            return ResponseEntity.ok(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity getFuturesResults(List<Future<ResponseEntity>> futures) {
        int unauthorized = 0;
        int errors = 0;
        int timeouts = 0;

        final int listSize = futures.size();
        ResponseEntity responseEntity = null;
        try {
            int size = listSize;
            while (size > 0) {
                for (int i = 0; i < size; i++) {
                    if (futures.get(i).isDone()) {
                        responseEntity = futures.get(i).get();
                        switch (responseEntity.getStatusCode()) {
                            case OK:
                                return responseEntity;
                            case UNAUTHORIZED:
                                unauthorized++;
                                futures.remove(i);
                                break;
                            case REQUEST_TIMEOUT:
                                timeouts++;
                                futures.remove(i);
                                break;
                            case INTERNAL_SERVER_ERROR:
                                errors++;
                                futures.remove(i);
                                break;
                            default:
                                futures.remove(i);
                                break;
                        }
                        break;
                    }
                }
                size = futures.size();
            }

            if (errors == listSize || unauthorized == listSize || timeouts == listSize) {
                return responseEntity;
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean isGuest(LoginForm loginForm) {
        String guestUsername = System.getenv("GUEST_USERNAME");
        String guestPassword = System.getenv("GUEST_PASSWORD");
        return loginForm.getUsername().equals(guestUsername) && loginForm.getPassword().equals(guestPassword);
    }
}
