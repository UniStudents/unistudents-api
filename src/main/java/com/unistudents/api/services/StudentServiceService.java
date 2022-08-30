package com.unistudents.api.services;

import com.unistudents.api.components.LoginForm;
import gr.unistudents.services.student.StudentService;
import gr.unistudents.services.student.components.Options;
import gr.unistudents.services.student.exceptions.*;
import gr.unistudents.services.student.components.StudentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class StudentServiceService {

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
            return new ResponseEntity<>(response, HttpStatus.OK);
        }  catch (InvalidCredentialsException e) {
            if (e.getException() != null) e.getException().printStackTrace();
            else e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (NotAuthorizedException e) {
            if (e.getException() != null) e.getException().printStackTrace();
            else e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        } catch (NotReachableException e) {
            if (e.getException() != null) e.getException().printStackTrace();
            else e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
        } catch (ParserException e) {
            if (e.getException() != null) e.getException().printStackTrace();
            else e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ScraperException e) {
            if (e.getException() != null) e.getException().printStackTrace();
            else e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
