package com.cygnus.ipoten.ebook.controller;

import com.cygnus.ipoten.ebook.controller.response_form.EbookListResponseForm;
import com.cygnus.ipoten.ebook.service.EbookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class EbookController {

    private final EbookService ebookService;

    @GetMapping("/ebooks")
    public ResponseEntity<EbookListResponseForm> listEbooksPublic() {
        var list = ebookService.listAll();
        return ResponseEntity.ok(EbookListResponseForm.from(list));
    }
}
