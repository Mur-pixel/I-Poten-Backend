package com.cygnus.iptn.exception;

import com.cygnus.iptn.account.exception.NotLoggedInException;
import com.cygnus.iptn.account.exception.UserNotFoundException;
import com.cygnus.iptn.authentication.social.SocialLoginErrorResponse;
import com.cygnus.iptn.authentication.social.SocialLoginException;
import com.cygnus.iptn.google_authentication.exception.GoogleAccessTokenException;
import com.cygnus.iptn.google_authentication.exception.GoogleGetUserInfoException;
import com.cygnus.iptn.term.exception.TermNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TermNotFoundException.class)
    public ResponseEntity<String> handleTermNotFoundException(TermNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("bad request", ex);
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResourceFoundException(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(SocialLoginException.class)
    public ResponseEntity<SocialLoginErrorResponse> handleSocialLoginException(SocialLoginException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ex.toResponse());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpectedException(Exception ex) {
        log.error("unexpected server error", ex);
        return ResponseEntity.internalServerError().body("서버 내부 오류가 발생했습니다.");
    }

    @ExceptionHandler(NotLoggedInException.class)
    public ResponseEntity<String> handleNotLoggedInException(NotLoggedInException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFoundException(UserNotFoundException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(GoogleAccessTokenException.class)
    public ResponseEntity<String> handleGoogleAccessTokenException(GoogleAccessTokenException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(GoogleGetUserInfoException.class)
    public ResponseEntity<String> handleGoogleGetUserInfoException(GoogleGetUserInfoException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }
}
