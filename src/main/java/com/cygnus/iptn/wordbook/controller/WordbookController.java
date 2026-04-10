package com.cygnus.iptn.wordbook.controller;

import com.cygnus.iptn.common.annotation.LoginUser;
import com.cygnus.iptn.wordbook.controller.request_form.BulkDeleteWordbookRequestForm;
import com.cygnus.iptn.wordbook.controller.request_form.CreateWordbookRequestForm;
import com.cygnus.iptn.wordbook.controller.request_form.RenameWordbookRequestForm;
import com.cygnus.iptn.wordbook.controller.request_form.ReorderWordbookRequestForm;
import com.cygnus.iptn.wordbook.controller.response_form.CreateWordbookResponseForm;
import com.cygnus.iptn.wordbook.controller.response_form.RenameWordbookResponseForm;
import com.cygnus.iptn.wordbook.repository.WordbookRepository;
import com.cygnus.iptn.wordbook.service.WordbookQueryService;
import com.cygnus.iptn.wordbook.service.WordbookService;
import com.cygnus.iptn.wordbook.service.request.CreateWordbookRequest;
import com.cygnus.iptn.wordbook.service.response.CreateWordbookResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Wordbook", description = "단어장 폴더 관리 API")
public class WordbookController {

    private final WordbookService wordbookService;
    private final WordbookRepository wordbookRepository;
    private final WordbookQueryService wordbookQueryService;

    @Operation(summary = "단어장 폴더 생성")
    @PostMapping("/me/folders")
    public CreateWordbookResponseForm createFolder(
            @LoginUser Long accountId,
            @RequestBody @Valid CreateWordbookRequestForm requestForm) {

        try {
            CreateWordbookRequest request = requestForm.toCreateFolderRequest(accountId);
            CreateWordbookResponse response = wordbookService.registerWordbook(request);
            log.info("[wordbook:create] done");
            return CreateWordbookResponseForm.from(response);
        } catch (Exception e) {
            log.error("[wordbook:create] 폴더 생성 중 오류 발생", e);
            throw e;
        }
    }

    @Operation(summary = "단어장 폴더 순서 재정렬")
    @PatchMapping("/me/folders:reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderFolders(
            @LoginUser Long accountId,
            @RequestBody @Valid ReorderWordbookRequestForm requestForm) {

        wordbookService.reorder(requestForm.toRequest(accountId));
    }

    @Operation(summary = "단어장 폴더 목록 조회(간단형)")
    @GetMapping({"/me/folders", "/user-terms/folders"})
    public List<Map<String, Object>> listFolders(@LoginUser Long accountId) {
        var list = wordbookRepository.findAllByAccount_IdOrderBySortOrderAscIdAsc(accountId);
        var result = list.stream()
                .map(f -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", f.getId());
                    m.put("wordbookName", f.getWordbookName());
                    m.put("sortOrder", f.getSortOrder());
                    return m;
                })
                .collect(Collectors.toList());
        log.info("[wordbook:list] done");
        return result;
    }

    @Operation(summary = "단어장 폴더 이름 변경")
    @PatchMapping("/me/folders/{wordbookId}")
    public RenameWordbookResponseForm renameFolder(
            @LoginUser Long accountId,
            @PathVariable Long wordbookId,
            @RequestBody @Valid RenameWordbookRequestForm requestForm) {

        log.debug("[wordbook:rename] wordbookId={}", wordbookId);
        var request = requestForm.toRequest(accountId, wordbookId);
        var response = wordbookService.rename(request);
        log.debug("[wordbook:rename] response={}", response);
        return RenameWordbookResponseForm.from(response);
    }

    @Operation(summary = "단어장 폴더 삭제(단건)")
    @DeleteMapping("/me/folders/{wordbookId}")
    public ResponseEntity<Void> deleteFolder(
            @LoginUser Long accountId,
            @PathVariable Long wordbookId,
            @RequestParam(name = "mode", defaultValue = "purge") String mode,
            @RequestParam(name = "targetWordbookId", required = false) Long targetWordbookId) {

        wordbookService.deleteOne(
                accountId,
                WordbookService.DeleteMode.of(mode),
                wordbookId,
                targetWordbookId
        );
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "단어장 폴더 삭제(다건)")
    @DeleteMapping("/me/folders:bulk")
    public ResponseEntity<Void> deleteFoldersBulk(
            @LoginUser Long accountId,
            @RequestParam(name = "mode", defaultValue = "purge") String mode,
            @RequestParam(name = "targetWordbookId", required = false) Long targetWordbookId,
            @RequestBody @Valid BulkDeleteWordbookRequestForm form) {

        var ids = form.getWordbookIds();
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        wordbookService.deleteBulk(
                accountId,
                WordbookService.DeleteMode.of(mode),
                ids.stream().distinct().toList(),
                targetWordbookId
        );
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "단어장 폴더 내 termId 전체 조회(PDF용)")
    @GetMapping("/me/folders/{wordbookId}/term-ids")
    public ResponseEntity<?> getAllTermIds(
            @LoginUser Long accountId,
            @PathVariable Long wordbookId) {

        var result = wordbookService.getAllTermIds(accountId, wordbookId);

        if (result.limitExceeded()) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .header("Ebook-Error", "LIMIT_EXCEEDED")
                    .header("Ebook-Limit", String.valueOf(result.limit()))
                    .header("Ebook-Total", String.valueOf(result.total()))
                    .body("LIMIT_EXCEEDED");
        }
        if (result.termIds().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .header("Ebook-Error", "EMPTY_WORDBOOK")
                    .body("EMPTY_WORDBOOK");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("wordbookId", result.wordbookId());
        body.put("count", result.termIds().size());
        body.put("termIds", result.termIds());
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "단어장 폴더별 즐겨찾기 단어 수 조회")
    @GetMapping("/me/wordbook/folders")
    public ResponseEntity<?> getMyFolders(@LoginUser Long accountId) {
        try {
            return ResponseEntity.ok(wordbookQueryService.getMyFolders(accountId));
        } catch (Exception e) {
            log.error("폴더 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
