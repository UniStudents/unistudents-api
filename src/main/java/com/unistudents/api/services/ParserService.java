package com.unistudents.api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unistudents.api.common.Logger;
import gr.unistudents.services.student.StudentService;
import gr.unistudents.services.student.components.Options;
import gr.unistudents.services.student.components.ScraperOutput;
import gr.unistudents.services.student.exceptions.ParserException;
import gr.unistudents.services.student.models.Student;
import org.jsoup.Jsoup;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ParserService {

    public ResponseEntity<Object> parse(String university, String system, JsonNode output) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ScraperOutput so = new ScraperOutput();
        if (output.has("infoJSON")) {
            so.infoJSON = mapper.writeValueAsString(output.get("infoJSON"));
        }
        if (output.has("gradesJSON")) {
            so.gradesJSON = mapper.writeValueAsString(output.get("gradesJSON"));
        }
        if (output.has("infoPage")) {
            so.infoPage = Jsoup.parse(output.get("infoPage").asText());
        }
        if (output.has("gradesPage")) {
            so.gradesPage = Jsoup.parse(output.get("gradesPage").asText());
        }
        if (output.has("allTrialsJSON")) {
            so.allTrialsJSON = mapper.writeValueAsString(output.get("allTrialsJSON"));
        }
        if (output.has("infoAndGradesPage")) {
            so.infoAndGradesPage = Jsoup.parse(output.get("infoAndGradesPage").asText());
        }
        if (output.has("infoAndGradesPageStr")) {
            so.infoAndGradesPageStr = output.get("infoAndGradesPageStr").asText();
        }
        if (output.has("totalAverageGrade")) {
            so.totalAverageGrade = output.get("totalAverageGrade").asText();
        }
        if (output.has("declareHistoryPage")) {
            so.declareHistoryPage = Jsoup.parse(output.get("declareHistoryPage").asText());
        }

        Options opts = new Options();
        opts.university = university;
        opts.system = system;

        long timestamp = System.currentTimeMillis();

        try {
            StudentService studentService = new StudentService(opts);
            Student student = studentService.parse(so);

            Logger.log(Logger.Type.PARSER, opts, null, null, timestamp);

            return new ResponseEntity<>(student, HttpStatus.OK);
        } catch (ParserException e) {
            // Get exception
            Throwable th = e;
            if (e.exception != null)
                th = e.exception;

            // Print stack trace
            th.printStackTrace();
            Logger.log(Logger.Type.PARSER, opts, e, e.exception, timestamp);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            // Print stack trace
            e.printStackTrace();
            Logger.log(Logger.Type.PARSER, opts, e, null, timestamp);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
