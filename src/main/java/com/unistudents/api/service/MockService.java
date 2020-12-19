package com.unistudents.api.service;

import com.unistudents.api.model.Student;
import com.unistudents.api.model.StudentDTO;
import com.unistudents.api.parser.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class MockService {

    public ResponseEntity getStudent(String university, String system) {
        if (system == null)
            return getStudent(university);

        switch (university) {
            case "AEGEAN":
                switch (system) {
                    case "CARDISOFT":
                        return getCARDISOFTStudent();
                    case "SEF":
                        return null;
                    case "ICARUS":
                        return getICARUSStudent();
                    default:
                        return new ResponseEntity(HttpStatus.NOT_FOUND);
                }
            case "AUA":
                switch (system) {
                    case "ILYDA":
                        return getILYDAStudent();
                    case "CUSTOM":
                        return getAUACustomStudent();
                    default:
                        System.out.println("here?");
                        return new ResponseEntity(HttpStatus.NOT_FOUND);
                }
            default:
                System.out.println("Or here??");
                return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    private ResponseEntity getStudent(String university) {
        switch (university) {
            case "UOA":
                return getUOAStudent();
            case "PANTEION":
                return getPANTEIONStudent();
            case "UPATRAS":
                return getUPATRASStudent();
            case "AUEB":
                return getARCHIMEDIAStudent();
            case "HUA":
                return getARCHIMEDIAStudent();
            case "NTUA":
                return getNTUAStudent();
            case "IHU":
                return getCARDISOFTStudent();
            case "UOP":
                return getCARDISOFTStudent();
            case "UOI":
                return getILYDAStudent();
            case "UNIWA":
                return getILYDAStudent();
            case "UNIPI":
                return getCARDISOFTStudent();
            case "UOC":
                return getCARDISOFTStudent();
            case "TUC":
                return getCARDISOFTStudent();
            case "UOWM":
                return getCARDISOFTStudent();
            case "HMU":
                return getCARDISOFTStudent();
            case "IONIO":
                return getCARDISOFTStudent();
            case "ASPETE":
                return getCARDISOFTStudent();
            default:
                System.out.println("Whaaat?");
                return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    private ResponseEntity getUOAStudent() {
        try {
            String htmlInfo = readFile("src/main/resources/UOA/uoaInfo.txt");
            String htmlGrades = readFile("src/main/resources/UOA/uoaGrades.txt");
            String htmlHistory = readFile("src/main/resources/UOA/uoa-declareHistory.txt");

            Document infoPage = Jsoup.parse(htmlInfo);
            Document gradesPage = Jsoup.parse(htmlGrades);
            Document declareHistoryPage = Jsoup.parse(htmlHistory);

            UOAParser parser = new UOAParser();
            Student student = parser.parseInfoAndGradesPages(infoPage, gradesPage, declareHistoryPage);
            student.getInfo().setAem("Sps1234567");

            StudentDTO studentDTO = new StudentDTO(null, null, student);

            return new ResponseEntity(studentDTO, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ResponseEntity getPANTEIONStudent() {
        try {
            String html = readFile("src/main/resources/PANTEION/panteion-student.html");

            Document[] infoAndGradesPages = new Document[]{Jsoup.parse(html)};

            PANTEIONParser parser = new PANTEIONParser();
            Student student = parser.parseInfoAndGradesPages(infoAndGradesPages);

            StudentDTO studentDTO = new StudentDTO(null, null, student);

            return new ResponseEntity(studentDTO, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ResponseEntity getUPATRASStudent() {
        try {
            String html = readFile("src/main/resources/UPATRAS/student.html");
            Document infoAndGradesPage = Jsoup.parse(html.substring(html.indexOf("<![CDATA[")+9, html.indexOf("]]>")));

            UPATRASParser parser = new UPATRASParser();
            Student student = parser.parseInfoAndGradesPage(infoAndGradesPage);

            StudentDTO studentDTO = new StudentDTO(null, null, student);

            return new ResponseEntity(studentDTO, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ResponseEntity getARCHIMEDIAStudent() {
        try {
            String html = readFile("src/main/resources/AUEB/aueb-student.xml");
            Document document = Jsoup.parse(html);

            ARCHIMEDIAParser parser = new ARCHIMEDIAParser();
            Student student = parser.parseInfoAndGradesPages(document);

            StudentDTO studentDTO = new StudentDTO(null, null, student);

            return new ResponseEntity(studentDTO, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ResponseEntity getNTUAStudent() {
        try {
            String html = readFile("src/main/resources/NTUA/ntua-ece.html");
            Document infoAndGradePage = Jsoup.parse(html);

            String infoAndGradesJSON = readFile("src/main/resources/NTUA/ntua-student.json");

            NTUAParser parser = new NTUAParser();
            Student student = parser.parseJSONAndDocument(infoAndGradesJSON, infoAndGradePage);

            StudentDTO studentDTO = new StudentDTO(null, null, student);

            return new ResponseEntity(studentDTO, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ResponseEntity getCARDISOFTStudent() {
        try {
            String htmlInfo = readFile("src/main/resources/UOC/uoc-info.html");
            String htmlGrades = readFile("src/main/resources/UOC/uoc.html");

            Document documentInfo = Jsoup.parse(htmlInfo);
            Document documentGrades = Jsoup.parse(htmlGrades);

            CardisoftParser parser = new CardisoftParser("MOCK", null);
            Student student = parser.parseInfoAndGradesPages(documentInfo, documentGrades);

            StudentDTO studentDTO = new StudentDTO(null, null, student);

            return new ResponseEntity(studentDTO, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ResponseEntity getILYDAStudent() {
        try {
            String infoJSON = readFile("src/main/resources/UOI/profiles.json");
            String gradesJSON = readFile("src/main/resources/UOI/diploma.json");
            String totalAverageGrade = readFile("src/main/resources/UOI/average_student_course_grades.json");

            ILYDAParser parser = new ILYDAParser("MOCK", null);
            Student student = parser.parseInfoAndGradesJSON(infoJSON, gradesJSON, totalAverageGrade);

            StudentDTO studentDTO = new StudentDTO(null, null, student);

            return new ResponseEntity(studentDTO, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ResponseEntity getAUACustomStudent() {
        try {
            String htmlInfo = readFile("src/main/resources/AUA/stud_studentOverview.html");
            String htmlGrades = readFile("src/main/resources/AUA/stud_showStudentGrades.html");

            Document infoPage = Jsoup.parse(htmlInfo);
            Document gradesPage = Jsoup.parse(htmlGrades);

            AUAParser parser = new AUAParser();
            Student student = parser.parseInfoAndGradesPages(infoPage, gradesPage);

            StudentDTO studentDTO = new StudentDTO(null, null, student);

            return new ResponseEntity(studentDTO, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ResponseEntity getICARUSStudent() {
        try {
            String html = readFile("src/main/resources/AEGEAN/icsd-icarus.html", "ISO-8859-7");

            Document infoAndGradePage = Jsoup.parse(html);

            ICARUSParser parser = new ICARUSParser();
            Student student = parser.parseInfoAndGradesPages(infoAndGradePage);

            StudentDTO studentDTO = new StudentDTO(null, null, student);

            return new ResponseEntity(studentDTO, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    private String readFile(String path, String charsetName) throws IOException {
        FileInputStream is = new FileInputStream(path);
        InputStreamReader isr = new InputStreamReader(is, charsetName);
        BufferedReader buffReader = new BufferedReader(isr);
        StringBuilder stringBuilder = new StringBuilder();
        String str;
        while ((str = buffReader.readLine()) != null) {
            stringBuilder.append(str);
        }
        return stringBuilder.toString();
    }
}
