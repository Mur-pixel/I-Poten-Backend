package com.cygnus.ipoten.wordbook_term.controller;


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
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "WordbookTerm", description = "단어장 내 단어 / 즐겨찾기 관리 API")
public class WordbookTermController {

    private final WordbookTermService wordbookTermService;
    private final WordbookService wordbookService;
    private final RedisCacheService redisCacheService;
    private final WordbookQueryService wordbookQueryService;
    private final WordbookLogService wordbookLogService;
    private final WordbookTermRepository wordbookTermRepository;

    @Operation(
            summary = "단어장 폴더에 단어 추가",
            description = "지정한 폴더에 termId 하나를 추가합니다."
    )
    @PostMapping("/me/folders/{folderId}/terms")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUserWordbookTermResponseForm addTermFolder(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long folderId,
            @RequestBody @Valid AddTermToWordbookRequestForm requestForm
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("[folder:attach] 인증 실패");
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }
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

    @Operation(
            summary = "단어장 폴더에 단어 일괄 추가",
            description = "termIds 배열을 전달하여 하나의 폴더에 여러 단어를 한 번에 추가합니다."
    )
    @PostMapping("/me/folders/{folderId}/terms:bulk")
    public ResponseEntity<AttachTermsBulkResponseForm> attachTermsBulk(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long folderId,
            @RequestBody @Valid AttachTermsBulkRequestForm requestForm
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }

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

    @Operation(
            summary = "폴더 간 단어 이동",
            description = "sourceWordbookId에 속한 단어들 중 일부를 targetWordbookId로 일괄 이동합니다."
    )
    @PatchMapping("/me/folders/{sourceWordbookId}/terms:move")
    public MoveWordbookTermsResponseForm moveFolderTerms(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long sourceWordbookId,
            @RequestBody @Valid MoveWordbookTermsRequestForm requestForm) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("[wordbooks:move] 인증 실패");
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }
        var response = wordbookTermService.moveTerms(accountId, sourceWordbookId, requestForm.getTargetWordbookId(), requestForm.getTermIds());
        log.info("[wordbooks:move] done");
        log.debug("[wordbooks:move] sourceWordbookId={}, targetWordbookId={}, count={}",
                sourceWordbookId, requestForm.getTargetWordbookId(), requestForm.getTermIds().size());
        return MoveWordbookTermsResponseForm.from(response);
    }

    @Operation(
            summary = "단어장 폴더 내 단어 개수 조회",
            description = "지정한 폴더에 포함된 단어의 총 개수만 빠르게 확인합니다."
    )
    @GetMapping("/me/folders/{folderId}/terms/count")
    public Map<String, Object> countFolderTerms(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long folderId
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }
        long count = wordbookQueryService.countTermsInFolderOrThrow(accountId, folderId);
        return Map.of("folderId",  folderId, "count", count);
    }

    @Operation(
            summary = "단어장 폴더 내 단어 목록 조회",
            description = "지정한 폴더에 포함된 단어들을 페이지네이션하여 조회합니다."
    )
    @GetMapping("/me/folders/{folderId}/terms")
    public ListWordbookTermResponseForm listFolderTerms(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int perPage,
            @RequestParam(required = false) String sort
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }

        var req = new ListWordbookTermRequest(
                accountId,
                folderId,
                page,
                perPage,
                sort
        );

        var res = wordbookService.list(req);
        return ListWordbookTermResponseForm.from(res);
    }

    @Operation(
            summary = "단어장 폴더에서 용어 제거",
            description = "단일 또는 여러 용어(termId)를 한 번에 폴더에서 제거합니다. termIds 배열 크기에 따라 단일/다건 모두 처리합니다."
    )
    @PatchMapping("/me/folders/{wordbookId}/terms:bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTermsFromWordbookBulk(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long wordbookId,
            @RequestBody @Valid BulkRemoveWordbookTermRequestForm requestForm
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("wordbook:remove-terms-bulk] 인증 실패");
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }

        var termIds = requestForm.getTermIds();
        if (termIds == null || termIds.isEmpty()) {
            log.debug("[wordbook:remove-terms-bulk] termIds 비어 있음 - Skip");
            return;
        }

        wordbookTermService.removeTermsFromWordbook(
                accountId,
                wordbookId,
                termIds.stream().distinct().toList()
        );
    }

    /**
     * 공통: userToken 쿠키에서 계정 ID를 조회한다.
     * - 토큰이 없거나 공백이면 null
     * - Redis에 없거나 TTL 만료된 경우도 null
     * → null이면 컨트롤러에서 UNAUTHORIZED 처리
     */
    private Long resolveAccountId(String userToken) {
        if (userToken == null || userToken.isBlank()) return null;
        return redisCacheService.getValueByKey(userToken, Long.class); // TTL 만료/무효면 null
    }
}
