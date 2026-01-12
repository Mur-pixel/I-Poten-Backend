package com.cygnus.ipoten.wordbook_pdf.service.export;

import com.cygnus.ipoten.wordbook_pdf.controller.export.request_form.TermsPdfGenerateByWordbookRequestForm;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public interface WordbookPdfExportApplicationService {
    ResponseEntity<StreamingResponseBody> generateByFolder(Long accountId, TermsPdfGenerateByWordbookRequestForm requestForm);
}
