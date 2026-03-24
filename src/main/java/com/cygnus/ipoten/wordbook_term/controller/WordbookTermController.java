package com.cygnus.ipoten.wordbook_term.controller;

import com.cygnus.ipoten.common.annotation.LoginUser;
import com.cygnus.ipoten.wordbook.controller.request_form.AddTermToWordbookRequestForm;
import com.cygnus.ipoten.wordbook.controller.request_form.AttachTermsBulkRequestForm;
import com.cygnus.ipoten.wordbook.controller.request_form.MoveWordbookTermsRequestForm;
import com.cygnus.ipoten.wordbook.controller.response_form.*;
import com.cygnus.ipoten.wordbook.service.WordbookQueryService;
import com.cygnus.ipoten.wordbook.service.WordbookService;
import com.cygnus.ipoten.wordbook_log.service.WordbookLogService;
import com.cygnus.ipoten.wordbook_term.controller.request_form.BulkRemoveWordbookTermRequestForm;
import com.cygnus.ipoten.wordbook_term.repository.WordbookTermRepository;
import com.cygnus.ipoten.wordbook_term.service.WordbookTermService;
import com.cygnus.ipoten.wordbook.service.request.AttachTermsBulkRequest;
import com.cygnus.ipoten.wordbook.service.request.CreateWordbookTermRequest;
import com.cygnus.ipoten.wordbook.service.request.ListWordbookTermRequest;
import com.cygnus.ipoten.wordbook.service.response.AttachTermsBulkResponse;
import com.cygnus.ipoten.wordbook.service.response.CreateWordbookTermResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "WordbookTerm", description = "단어장 내 단어 / 즐겨찾기 관리 API")
public class WordbookTermController {

    private final WordbookTermService wordbookTermService;
    private final WordbookService wordbookService;
    private final WordbookQueryService wordbookQueryService;
    private final WordbookLogService wordbookLogService;
    private final WordbookTermRepository wordbookTermRepository;

    @Operation(summary = "단어장 폴더에 단어 추가")
    @PostMapping("/me/folders/{folderId}/terms")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUserWordbookTermResponseForm addTermFolder(
            @LoginUser Long accountId,
            @PathVariable Long folderId,
            @RequestBody @Valid AddTermToWordbookRequestForm requestForm) {

        log.debug("[folder:attach] folderId={}, reqForm={}", folderId, requestForm);
        CreateWordbookTermRequest request = requestForm.toRequest(accountId, folderId);
        CreateWordbookTermResponse response = wordbookService.attachTerm(request);

        try {
            wordbookLogService.recordTermSaved(accountId, folderId, requestForm.getTermId());
        } catch (Exception e) {
            log.warn("[wordbook_log] TERM_SAVED failed (ignored). accountId={}, folderId={}, termId={}",
                    accountId, folderId, requestForm.getTermId(), e);
        }
        return CreateUserWordbookTermResponseForm.from(response);
    }

    @Operation(summary = "단어장 폴더에 단어 일괄 추가")
    @PostMapping("/me/folders/{folderId}/terms:bulk")
    public ResponseEntity<AttachTermsBulkResponseForm> attachTermsBulk(
            @LoginUser Long accountId,
            @PathVariable Long folderId,
            @RequestBody @Valid AttachTermsBulkRequestForm requestForm) {

        AttachTermsBulkRequest request = requestForm.toRequest(accountId, folderId);
        AttachTermsBulkResponse response = wordbookService.attachTermsBulk(request);

        try {
            int count = (requestForm.termIds() == null) ? 0
                    : (int) requestForm.termIds().stream().filter(java.util.Objects::nonNull).distinct().count();
            if (count > 0) {
                wordbookLogService.recordTermsSavedBulk(accountId, folderId, count);
            }
        } catch (Exception e) {
            log.warn("[wordbook_log] TERM_SAVED(BULK) failed (ignored). accountId={}, wordbookId={}",
                    accountId, folderId, e);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(AttachTermsBulkResponseForm.from(response));
    }

    @Operation(summary = "폴더 간 단어 이동")
    @PatchMapping("/me/folders/{sourceWordbookId}/terms:move")
    public MoveWordbookTermsResponseForm moveFolderTerms(
            @LoginUser Long accountId,
            @PathVariable Long sourceWordbookId,
            @RequestBody @Valid MoveWordbookTermsRequestForm requestForm) {

        var response = wordbookTermService.moveTerms(accountId, sourceWordbookId, requestForm.getTargetWordbookId(), requestForm.getTermIds());
        log.info("[wordbooks:move] done");
        return MoveWordbookTermsResponseForm.from(response);
    }

    @Operation(summary = "단어장 폴더 내 단어 개수 조회")
    @GetMapping("/me/folders/{folderId}/terms/count")
    public Map<String, Object> countFolderTerms(
            @LoginUser Long accountId,
            @PathVariable Long folderId) {

        long count = wordbookQueryService.countTermsInFolderOrThrow(accountId, folderId);
        return Map.of("folderId", folderId, "count", count);
    }

    @Operation(summary = "단어장 폴더 내 단어 목록 조회")
    @GetMapping("/me/folders/{folderId}/terms")
    public ListWordbookTermResponseForm listFolderTerms(
            @LoginUser Long accountId,
            @PathVariable Long folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int perPage,
            @RequestParam(required = false) String sort) {

        var req = new ListWordbookTermRequest(accountId, folderId, page, perPage, sort);
        var res = wordbookService.list(req);
        return ListWordbookTermResponseForm.from(res);
    }

    @Operation(summary = "단어장 폴더에서 용어 제거")
    @PatchMapping("/me/folders/{wordbookId}/terms:bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTermsFromWordbookBulk(
            @LoginUser Long accountId,
            @PathVariable Long wordbookId,
            @RequestBody @Valid BulkRemoveWordbookTermRequestForm requestForm) {

        var termIds = requestForm.getTermIds();
        if (termIds == null || termIds.isEmpty()) {
            log.debug("[wordbook:remove-terms-bulk] termIds 비어 있음 - Skip");
            return;
        }
        wordbookTermService.removeTermsFromWordbook(accountId, wordbookId, termIds.stream().distinct().toList());
    }
}
