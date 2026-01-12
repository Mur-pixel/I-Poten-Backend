package com.cygnus.ipoten.wordbook_pdf.service.export;

import com.cygnus.ipoten.wordbook_pdf.controller.export.request_form.TermsPdfGenerateByWordbookRequestForm;
import com.cygnus.ipoten.wordbook_pdf.service.export.response.PdfExportService;
import com.cygnus.ipoten.wordbook.service.WordbookQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;

import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordbookPdfExportApplicationServiceImpl implements WordbookPdfExportApplicationService {

    private final WordbookQueryService wordbookQueryService;
    private final PdfExportService pdfExportService;

    @Override
    public ResponseEntity<StreamingResponseBody> generateByFolder(
            Long accountId,
            TermsPdfGenerateByWordbookRequestForm requestForm
    ) {
        // 1) termId 수집(필터/정렬/상한)
        final var filters = requestForm.getFilters();
        final var collected = wordbookQueryService.collectExportTermIds(
                accountId,
                requestForm.getWordbookId(),
                (filters == null) ? null : filters.getMemorization(),
                (filters == null) ? null : filters.getIncludeTags(),
                (filters == null) ? null : filters.getExcludeTags(),
                requestForm.getSort(),
                0 // hardLimit 미지정 -> 서비스 내부 기본 상한 사용
        );

        // 1-1) 상한 초과 → 413
        if (collected.limitExceeded()) {
            return ResponseEntity.status(PAYLOAD_TOO_LARGE)
                    .header("Ebook-Error", "LIMIT_EXCEEDED")
                    .header("Ebook-Limit", String.valueOf(collected.limit()))
                    .header("Ebook-Total", String.valueOf(collected.totalBeforeFilter()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .build();
        }

        // 1-2) 빈 결과 → 422
        if (collected.termIds().isEmpty()) {   // ← termsIds() 아님!
            return ResponseEntity.status(UNPROCESSABLE_ENTITY)
                    .header("Ebook-Error", "EMPTY_FOLDER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .build();
        }

        // 2) PDF 생성
        final String title = (requestForm.getTitle() == null || requestForm.getTitle().isBlank())
                ? "내 단어장 PDF"
                : requestForm.getTitle();

        final var generateReq = PdfGenerateRequest.builder()
                .accountId(accountId)
                .termIds(collected.termIds())
                .title(title)
                .build();

        final var render = pdfExportService.generate(generateReq);

        // 3) 파일명/CD 헤더
        final String rawFilename = (render.meta().getFilename() != null && !render.meta().getFilename().isBlank())
                ? render.meta().getFilename()
                : "jobspoon_terms_" + LocalDate.now() + ".pdf";
        final String contentDisposition = buildContentDisposition(rawFilename);

        // 4) 스트리밍 응답 + 메타 헤더
        final int skipped = Math.max(0, collected.filteredOutCount()); // 필터로 제외된 개수
        final StreamingResponseBody body = os -> {
            try {
                render.stream().writeTo(os);
            } catch (IOException e) {
                log.warn("PDF streaming failed: {}", e.getMessage(), e);
                throw e;
            } catch (Exception e) {
                log.error("Unexpected render error", e);
                throw new IOException("Render failed", e);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header("Ebook-Id", String.valueOf(render.meta().getWordbookPdfId()))
                .header("Ebook-Filename", rawFilename)
                .header("Ebook-Count", String.valueOf(render.meta().getCount()))
                .header("Ebook-Skipped", String.valueOf(skipped))
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }

    /** RFC 5987 + ASCII fallback(Content-Disposition) */
    private static String buildContentDisposition(String raw) {
        String asciiFallback = Normalizer.normalize(raw, Normalizer.Form.NFKD)
                .replaceAll("[^\\p{ASCII}]", "_");
        if (!asciiFallback.endsWith(".pdf")) asciiFallback += ".pdf";
        String encoded = URLEncoder.encode(raw, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encoded;
    }
}
