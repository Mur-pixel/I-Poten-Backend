package com.cygnus.ipoten.ebook.controller;

import com.cygnus.ipoten.ebook.controller.response_form.EbookListResponseForm;
import com.cygnus.ipoten.ebook.service.EbookService;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class EbookController {

    private final EbookService ebookService;
    private final RedisCacheService redisCacheService;

    @Operation(
            summary = "포텐북 목록 조회(공개)",
            description = "로그인 없이 전체 포텐북 목록을 조회합니다."
    )
    @GetMapping("/ebooks")
    public ResponseEntity<EbookListResponseForm> listEbooksPublic() {
        var list = ebookService.listAll();
        return ResponseEntity.ok(EbookListResponseForm.from(list));
    }

    @Operation(
            summary = "전자책 PDF 파일 스트리밍(로그인 필요)",
            description = "로그인한 사용자만 PDF 파일을 Range(부분 요청) 기반으로 스트리밍합니다."
    )
    @GetMapping("/me/ebooks/{ebookId}/file")
    public ResponseEntity<?> streamEbookPdf(
            @PathVariable Long ebookId,
            @RequestHeader HttpHeaders headers,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            Path filePath = ebookService.resolveFilePathOrThrow(ebookId);
            Resource resource = new FileSystemResource(filePath.toFile());
            if (!resource.exists()) return ResponseEntity.notFound().build();

            long contentLength = resource.contentLength();

            List<HttpRange> ranges = headers.getRange();
            if (ranges == null || ranges.isEmpty()) {
                // Range 없으면 전체 파일
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .contentLength(contentLength)
                        .cacheControl(CacheControl.noStore())
                        .body(resource);
            }

            // Range 있으면 부분 스트리밍(1MB)
            long chunkSize = 1024 * 1024;
            HttpRange range = ranges.get(0);

            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long length = Math.min(chunkSize, (end - start + 1));

            ResourceRegion region = new ResourceRegion(resource, start, length);

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE,
                            "bytes " + start + "-" + (start + length - 1) + "/" + contentLength)
                    .contentLength(length)
                    .cacheControl(CacheControl.noStore())
                    .body(region);

        } catch (Exception e) {
            log.error("streamEbookPdf failed ebookId={}", ebookId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Long resolveAccountId(String userToken) {
        if (userToken == null || userToken.isBlank()) return null;
        return redisCacheService.getValueByKey(userToken, Long.class);
    }
}
