package com.cygnus.ipoten.exception;

import com.cygnus.ipoten.account.exception.NotLoggedInException;
import com.cygnus.ipoten.account.exception.UserNotFoundException;
import com.cygnus.ipoten.term.exception.TermNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 Not Found 처리
    @ExceptionHandler(TermNotFoundException.class)
    public ResponseEntity<String> handleTermNotFoundException(TermNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    // 400 Bad Request 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    // (선택) 예상치 못한 서버 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpectedException(Exception ex) {
        return ResponseEntity.internalServerError().body("서버 내부 오류가 발생했습니다.");
    }

    @ExceptionHandler(NotLoggedInException.class)
    public ResponseEntity<String> handleNotLoggedInException(NotLoggedInException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED); // 401 Unauthorized
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFoundException(UserNotFoundException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND); // 404 Not Found
    }
}