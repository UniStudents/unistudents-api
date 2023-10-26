package com.unistudents.api.services;

import gr.unistudents.services.student.components.Options;
import gr.unistudents.services.student.components.ScraperOutput;
import gr.unistudents.services.student.components.University;
import gr.unistudents.services.student.exceptions.NotAuthorizedException;
import gr.unistudents.services.student.exceptions.NotReachableException;
import gr.unistudents.services.student.exceptions.ParserException;
import gr.unistudents.services.student.exceptions.ScraperException;
import gr.unistudents.services.student.models.Student;
import gr.unistudents.services.student.parsers.UPATRASParser;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ParserService {

    private final Logger logger = LoggerFactory.getLogger(ParserService.class);

    public ResponseEntity<Object> get(String university, String body) {
        try {
            ScraperOutput so = new ScraperOutput();
            so.infoAndGradesPage = Jsoup.parse(body);

            Options opts = new Options();

            if(Objects.equals(university, "upatras.gr")) {
                opts.university = "upatras.gr";
//                opts.system = "progress";
                Student student = new UPATRASParser(new University(opts)).parse(so);
                return new ResponseEntity<>(student, HttpStatus.OK);
            } else {
                throw new Exception("Not supported for standalone parsing");
            }
        } catch (NotAuthorizedException e) {
            // Get exception
            Throwable th = e;
            if (e.exception != null)
                th = e.exception;

            // Print stack trace
            th.printStackTrace();

            Sentry.captureException(th, scope -> {
                scope.setTag("university", university);
                scope.setTag("exception-class", "NotAuthorizedException");
                scope.setLevel(SentryLevel.WARNING);
            });
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (NotReachableException e) {
            // Get exception
            Throwable th = e;
            if (e.exception != null)
                th = e.exception;

            // Print stack trace
            th.printStackTrace();

            // Send to analytics
            Sentry.captureException(th, scope -> {
                scope.setTag("university", university);
                scope.setTag("exception-class", "NotReachableException");
                scope.setLevel(SentryLevel.WARNING);
            });
            logger.warn("[" + university + "] Not reachable: " + e.getMessage(), th);

            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        } catch (ParserException e) {
            // Get exception
            Throwable th = e;
            if (e.exception != null)
                th = e.exception;

            // Print stack trace
            th.printStackTrace();

            // if (e.document != null && e.document.length() > 0) {
            //     String docUrl = this.uploadDocuments(university, system, e.document);

            //     Attachment attachment = new Attachment(e.document.getBytes(StandardCharsets.UTF_8), "document.txt");
            //     Hint hint = new Hint();
            //     hint.addAttachment(attachment);
            //     Sentry.captureException(th, hint, scope -> {
            //         scope.setTag("university", university);
            //         scope.setTag("exception-class", "ParserException");
            //         scope.setTag("docUrl", docUrl != null ? docUrl : "null");
            //         scope.setLevel(SentryLevel.ERROR);
            //     });
            // } else {
            Sentry.captureException(th, scope -> {
                scope.setTag("university", university);
                scope.setTag("exception-class", "ParserException");
                scope.setLevel(SentryLevel.ERROR);
            });
            // }

            // Send to analytics
            logger.error("[" + university + "] Parser error: " + e.getMessage(), th);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ScraperException e) {
            // Get exception
            Throwable th = e;
            if (e.exception != null)
                th = e.exception;

            // Print stack trace
            th.printStackTrace();

            // Send to analytics
            Sentry.captureException(th, scope -> {
                scope.setTag("university", university);
                scope.setTag("exception-class", "ScraperException");
                scope.setLevel(SentryLevel.ERROR);
            });
            logger.error("[" + university + "] Scraper error: " + e.getMessage(), th);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            // Print stack trace
            e.printStackTrace();

            // Send to analytics
            Sentry.captureException(e, scope -> {
                scope.setTag("university", university);
                scope.setTag("exception-class", "Exception");
                scope.setLevel(SentryLevel.ERROR);
            });
            logger.error("[" + university + "] General error: " + e.getMessage(), e);

            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
