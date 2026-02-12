package com.cygnus.ipoten.wordbook_pdf.controller.export;

import com.cygnus.ipoten.wordbook_log.service.WordbookLogService;
import com.cygnus.ipoten.wordbook_pdf.controller.export.request_form.TermsPdfGenerateRequestForm;
import com.cygnus.ipoten.wordbook_pdf.controller.export.response_form.TermsPdfGenerateResponseForm;
import com.cygnus.ipoten.wordbook_pdf.service.WordbookPdfEraseService;
import com.cygnus.ipoten.wordbook_pdf.service.export.response.PdfExportService;
import com.cygnus.ipoten.wordbook_pdf.service.export.PdfGenerateRequest;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import com.cygnus.ipoten.wordbook.service.WordbookQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class WordbookPdfExportController {

    private final PdfExportService pdfExportService;
    private final RedisCacheService redisCacheService;
    private final WordbookPdfEraseService wordbookPdfEraseService;
    private final WordbookQueryService wordbookQueryService;
    private final WordbookLogService wordbookLogService;

    /** 공통: 쿠키에서 토큰 추출 후 Redis에서 accountId 조회(없으면 null) — 쿠키 전용 */
    private Long resolveAccountId(String userToken) {
        if (userToken == null || userToken.isBlank()) return null;
        return redisCacheService.getValueByKey(userToken, Long.class); // TTL 만료/무효면 null
    }

    @PostMapping(value = "/pdf/generate", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<StreamingResponseBody> generate(
            @Valid @RequestBody TermsPdfGenerateRequestForm form,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 토큰이 없습니다. 요청을 거부합니다");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        final boolean hasTermIds =
                form.getTermIds() != null && !form.getTermIds().isEmpty();
        final Long wordbookId =
                form.getWordbookId() != null ? form.getWordbookId()
                        : form.getUserWordbookId();

        // termIds도 없고, wordbookId도 없으면 잘못된 요청
        if (!hasTermIds && wordbookId == null) {
            log.warn("요청 유효성 오류: termIds와 wordbookId가 모두 비어있음. form={}", form);
            return ResponseEntity.badRequest()
                    .header("Ebook-Error", "EMPTY_TARGET")
                    .build();
        }

        try {
            log.info("PDF(by-folder) 요청 - form={}", form);

            // 최종적으로 PdfExportService에 넘길 termIds
            final java.util.List<Long> termIds;

            if (hasTermIds) {
                // 선택 용어 기반
                termIds = form.getTermIds();
            } else {
                // 폴더 전체 기반 → 여기서 termIds 수집
                final var collected = wordbookQueryService.collectExportTermIds(
                        accountId,
                        wordbookId,
                        null,   // memorization filter 없음
                        null,   // includeTags 없음
                        null,   // excludeTags 없음
                        null,   // sort 없음 (기본 정렬)
                        0       // hardLimit 미지정 → 서비스 기본 상한
                );

                // 상한 초과 시 413 (기존 ApplicationService와 동일 정책)
                if (collected.limitExceeded()) {
                    return ResponseEntity.status(PAYLOAD_TOO_LARGE)
                            .header("Ebook-Error", "LIMIT_EXCEEDED")
                            .header("Ebook-Limit", String.valueOf(collected.limit()))
                            .header("Ebook-Total", String.valueOf(collected.totalBeforeFilter()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .build();
                }

                // 폴더가 비었으면 422
                if (collected.termIds().isEmpty()) {
                    return ResponseEntity.status(UNPROCESSABLE_ENTITY)
                            .header("Ebook-Error", "EMPTY_FOLDER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .build();
                }

                termIds = collected.termIds();
            }

            // 여기서는 무조건 termIds가 non-empty
            final PdfGenerateRequest request = PdfGenerateRequest.builder()
                    .accountId(accountId)
                    .termIds(termIds)
                    .title(form.getTitle())
                    .build();

            final var result = pdfExportService.generate(request);

            try {
                Long ebookId = result.meta().getWordbookPdfId();
                int count = result.meta().getCount();
                wordbookLogService.recordPdfDownloaded(accountId, wordbookId, count, ebookId);
            } catch (Exception e) {
                log.warn("[wordbook_log] PDF_DOWNLOADED failed (ignored). wordbookId={}", wordbookId, e);
            }

            final var responseForm = TermsPdfGenerateResponseForm.builder()
                    .ebookId(result.meta().getWordbookPdfId())
                    .filename(result.meta().getFilename())
                    .count(result.meta().getCount())
                    .build();

            StreamingResponseBody body = os -> {
                try {
                    log.debug("PDF 스트리밍 시작");
                    result.stream().writeTo(os);
                    log.debug("PDF 스트리밍 완료");
                } catch (IOException e) {
                    log.warn("PDF 스트리밍 중 IOException: {}", e.getMessage(), e);
                    throw e;
                } catch (Exception e) {
                    log.error("예상치 못한 렌더링 오류가 발생했습니다.", e);
                    throw new IOException("렌더링에 실패했습니다.", e);
                }
            };

            String filename = (responseForm.getFilename() != null && !responseForm.getFilename().isBlank())
                    ? responseForm.getFilename()
                    : "I-Poten_terms_" + LocalDate.now() + ".pdf";

            ContentDisposition cd = ContentDisposition.attachment()
                    .filename(filename, StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                    .header("Ebook-Id", String.valueOf(responseForm.getEbookId()))
                    .header("Ebook-Filename", filename)
                    .header("Ebook-Count", String.valueOf(responseForm.getCount()))
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(body);

        } catch (Exception e) {
            log.error("PDF 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Ebook-Error", "GENERATE_FAILED")
                    .build();
        }
    }

    /**
     * 내부(Admin) 호출용: ebook 도메인 데이터(해당 계정 것만) 삭제
     */
    @DeleteMapping("/internal/admin/accounts/{accountId}/ebooks:erase")
    public ResponseEntity<?> eraseEbooksByAccount(@PathVariable Long accountId) {
        var result = wordbookPdfEraseService.eraseByAccountId(accountId);

        Map<String, Object> body = Map.of(
                "accountId", accountId,
                "deleted", Map.of(
                        "ebook", result.getEbooks()
                )
        );

        log.info("[ebook:erase] {}", body);
        return ResponseEntity.ok(body);
    }
}
