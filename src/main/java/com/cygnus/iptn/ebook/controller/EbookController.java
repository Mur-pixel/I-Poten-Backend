package com.cygnus.iptn.ebook.controller;

import com.cygnus.iptn.common.annotation.LoginUser;
import com.cygnus.iptn.common.annotation.PublicEndpoint;
import com.cygnus.iptn.ebook.controller.response_form.EbookListResponseForm;
import com.cygnus.iptn.ebook.service.EbookService;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class EbookController {

    private final EbookService ebookService;

    @PublicEndpoint
    @Operation(summary = "포텐북 목록 조회(공개)", description = "로그인 없이 전체 포텐북 목록을 조회합니다.")
    @GetMapping("/ebooks")
    public ResponseEntity<EbookListResponseForm> listEbooksPublic() {
        var list = ebookService.listAll();
        return ResponseEntity.ok(EbookListResponseForm.from(list));
    }

    @Operation(summary = "전자책 PDF 파일 스트리밍(로그인 필요)")
    @GetMapping("/me/ebooks/{ebookId}/file")
    public ResponseEntity<?> streamEbookPdf(
            @PathVariable Long ebookId,
            @RequestHeader HttpHeaders headers,
            @LoginUser Long accountId) {

        try {
            Path filePath = ebookService.resolveFilePathOrThrow(ebookId);
            Resource resource = new FileSystemResource(filePath.toFile());
            if (!resource.exists()) return ResponseEntity.notFound().build();

            long contentLength = resource.contentLength();

            List<HttpRange> ranges = headers.getRange();
            if (ranges == null || ranges.isEmpty()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .contentLength(contentLength)
                        .cacheControl(CacheControl.noStore())
                        .body(resource);
            }

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
}
