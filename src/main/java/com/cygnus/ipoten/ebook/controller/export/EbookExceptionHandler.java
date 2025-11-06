package com.cygnus.ipoten.ebook.controller.export;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Slf4j
@RestControllerAdvice(basePackages = "com.wowraid.jobspoon.ebook")
public class EbookExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleRse(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .header("Ebook-Error", safe(e.getReason()))
                .body(e.getReason());
    }
    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleIo(IOException e) {
        log.warn("[ebook] IO error", e);
        return ResponseEntity.status(500).header("Ebook-Error", "IO_ERROR").body("IO_ERROR");
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleEtc(Exception e) {
        log.error("[ebook] Unexpected", e);
        return ResponseEntity.status(500).header("Ebook-Error", "UNKNOWN_ERROR").body("UNKNOWN_ERROR");
    }
    private String safe(String s){ return s==null?"ERROR":s.replaceAll("[^\\p{L}\\p{N}\\s._-]","").trim(); }
}
