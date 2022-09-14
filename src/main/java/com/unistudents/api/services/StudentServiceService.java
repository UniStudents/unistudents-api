package com.unistudents.api.services;

import com.unistudents.api.components.LoginForm;
import gr.unistudents.services.student.StudentService;
import gr.unistudents.services.student.components.Options;
import gr.unistudents.services.student.exceptions.*;
import gr.unistudents.services.student.components.StudentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class StudentServiceService {

    private final Logger logger = LoggerFactory.getLogger(StudentServiceService.class);

    private String getUniForLogs(String university, String system) {
        return university + (system != null && system.trim().length() != 0 ? "." + system : "");
    }
    
    private ResponseEntity<Object> guest(String university, String system, LoginForm loginForm) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    public ResponseEntity<Object> get(String university, String system, LoginForm loginForm) {
        if(university.toLowerCase(Locale.ROOT).equals("guest"))
            return guest(university, system, loginForm);

        Options options = new Options();
        options.university = university.toLowerCase(Locale.ROOT);
        options.system = system != null ? system.toLowerCase(Locale.ROOT) : null;
        options.username = loginForm.getUsername();
        options.password = loginForm.getPassword();
        options.cookies = loginForm.getCookies();

        try {
            StudentResponse response = StudentService.get(options);
            if (response.getCaptchaSystem() != null) {
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(response, HttpStatus.OK);
        }  catch (InvalidCredentialsException e) {
            // Get exception
            Throwable th = e;
            if(e.getException() != null)
                th = e.getException();

            // Print stack trace
            th.printStackTrace();

            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (NotAuthorizedException e) {
            // Get exception
            Throwable th = e;
            if(e.getException() != null)
                th = e.getException();

            // Print stack trace
            th.printStackTrace();

            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (NotReachableException e) {
            // Get exception
            Throwable th = e;
            if(e.getException() != null)
                th = e.getException();

            // Print stack trace
            th.printStackTrace();

            // Send to analytics
            logger.warn("[" + university + "] Not reachable: " + e.getMessage(), th);
            
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        } catch (ParserException e) {
            // Get exception
            Throwable th = e;
            if(e.getException() != null)
                th = e.getException();

            // Print stack trace
            th.printStackTrace();

            // Send to analytics
            logger.error("[" + getUniForLogs(university, system) + "] Parser error: " + e.getMessage(), th);
            
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ScraperException e) {
            // Get exception
            Throwable th = e;
            if(e.getException() != null)
                th = e.getException();

            // Print stack trace
            th.printStackTrace();

            // Send to analytics
            logger.error("[" + getUniForLogs(university, system) + "] Scraper error: " + e.getMessage(), th);
            
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
