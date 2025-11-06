package com.cygnus.ipoten.ebook.controller.export;

import com.cygnus.ipoten.ebook.controller.export.request_form.TermsPdfGenerateRequestForm;
import com.cygnus.ipoten.ebook.controller.export.response_form.TermsPdfGenerateResponseForm;
import com.cygnus.ipoten.ebook.service.EbookEraseService;
import com.cygnus.ipoten.ebook.service.export.dto.PdfExportService;
import com.cygnus.ipoten.ebook.service.export.dto.request.PdfGenerateRequest;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class EbookExportController {

    private final PdfExportService pdfExportService;
    private final RedisCacheService redisCacheService;
    private final EbookEraseService ebookEraseService;

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
        // 1) 토큰 선택 : 쿠키 전용(과도기 헤더 백업 제거)
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 토큰이 없습니다. 요청을 거부합니다");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.debug("계정 확인 완료 - 계정 ID: {}", accountId);

        // 3) 요청 유효성 검증
        if (form.getTermIds() == null || form.getTermIds().isEmpty()) {
            log.warn("요청 유효성 오류: termIds가 비어있음");
            return ResponseEntity.badRequest().build();
        }

        // 4) PDF 생성 + 응답 준비 (result 스코프 유지 위해 try 내부에서 처리)
        try {
            log.debug("PDF 생성 서비스 호출 중...");
            final var request = PdfGenerateRequest.builder()
                    .accountId(accountId)
                    .termIds(form.getTermIds())
                    .title(form.getTitle())
                    .build();

            final var result = pdfExportService.generate(request);

            final var responseForm = TermsPdfGenerateResponseForm.builder()
                    .ebookId(result.meta().getEbookId())
                    .filename(result.meta().getFilename())
                    .count(result.meta().getCount())
                    .build();

            log.info("PDF 생성 완료 - 전자책 ID: {}, 파일명: {}, 용어 수: {}",
                    responseForm.getEbookId(), responseForm.getFilename(), responseForm.getCount());

            // 스트리밍 본문
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

            // 파일명 및 헤더
            String filename = (responseForm.getFilename() != null && !responseForm.getFilename().isBlank())
                    ? responseForm.getFilename()
                    : "jobspoon_terms_" + LocalDate.now() + ".pdf";

            ContentDisposition cd = ContentDisposition.attachment()
                    .filename(filename, StandardCharsets.UTF_8) // filename & filename* 자동 처리
                    .build();

            log.info("PDF 응답 준비 완료 - 최종 파일명: {}", filename);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                    // 노출 헤더 표기 통일: Title-Case
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
        var result = ebookEraseService.eraseByAccountId(accountId);

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
