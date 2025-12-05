package com.cygnus.ipoten.wordbook.controller;


import com.cygnus.ipoten.wordbook.controller.request_form.AddTermToFolderRequestForm;
import com.cygnus.ipoten.wordbook.controller.request_form.AttachTermsBulkRequestForm;
import com.cygnus.ipoten.wordbook.controller.request_form.MoveFavoritesRequestForm;
import com.cygnus.ipoten.wordbook.controller.request_form.MoveFolderTermsRequestForm;
import com.cygnus.ipoten.wordbook.controller.response_form.*;
import com.cygnus.ipoten.wordbook.service.WordbookFolderQueryService;
import com.cygnus.ipoten.wordbook.service.WordbookFolderService;
import com.cygnus.ipoten.wordbook.service.WordbookTermService;
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
    private final WordbookFolderService wordbookFolderService;
    private final RedisCacheService redisCacheService;
    private final WordbookFolderQueryService wordbookFolderQueryService;

    @Operation(
            summary = "단어장 폴더에 단어 추가",
            description = "지정한 폴더에 termId 하나를 추가합니다."
    )
    @PostMapping("/me/folders/{folderId}/terms")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUserWordbookTermResponseForm addTermFolder(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long folderId,
            @RequestBody @Valid AddTermToFolderRequestForm requestForm
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("[folder:attach] 인증 실패");
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }
        log.debug("[folder:attach] accountId={}, folderId={}, reqForm={}", accountId, folderId, requestForm);
        CreateWordbookTermRequest request = requestForm.toRequest(accountId, folderId);
        CreateWordbookTermResponse response = wordbookFolderService.attachTerm(request);
        log.info("[folder:attach] done");
        log.debug("[folder:attach:res] {}", response);
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
        AttachTermsBulkResponse response = wordbookFolderService.attachTermsBulk(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(AttachTermsBulkResponseForm.from(response));
    }

    @Operation(
            summary = "즐겨찾기 용어 삭제",
            description = "현재 로그인한 사용자의 즐겨찾기(별표 단어장)에서 특정 termId를 제거합니다."
    )
    @DeleteMapping("/me/favorite-terms/by-term/{termId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavoriteByTerm(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long termId) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        wordbookTermService.removeFromStarFolder(accountId, termId);
    }

    @Operation(
            summary = "즐겨찾기 용어 다른 폴더로 이동",
            description = "즐겨찾기(별표 단어장)에 있던 단어들을 지정한 일반 단어장 폴더로 일괄 이동합니다."
    )
    @PatchMapping("/me/wordbook/favorites:move")
    public MoveFavoritesResponseForm moveFavorites(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody @Valid MoveFavoritesRequestForm requestForm) {

        Long accountId = resolveAccountId(userToken);
        if (accountId == null) throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");

        var termIds = requestForm.getTermIds();
        var targetFolderId = requestForm.getTargetFolderId();

        var serviceRes = wordbookTermService
                .moveFromStarFolder(accountId, targetFolderId, termIds);

        log.info("[favorites:move] done");
        log.debug("[favorites:move] accountId={}, targetFolderId={}, reqCount={}, moved={}",
                accountId, targetFolderId,
                (termIds == null ? 0 : termIds.size()),
                serviceRes.getMovedCount());

        return MoveFavoritesResponseForm.from(serviceRes);
    }

    @Operation(
            summary = "폴더 간 단어 이동",
            description = "sourceFolderId에 속한 단어들 중 일부를 targetFolderId로 일괄 이동합니다."
    )
    @PatchMapping("/me/folders/{sourceFolderId}/terms:move")
    public MoveFolderTermsResponseForm moveFolderTerms(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long sourceFolderId,
            @RequestBody @Valid MoveFolderTermsRequestForm requestForm) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("[folders:move] 인증 실패");
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }
        var response = wordbookFolderService.moveTerms(accountId, sourceFolderId, requestForm.getTargetFolderId(), requestForm.getTermIds());
        log.info("[folders:move] done");
        log.debug("[folders:move] accountId={}, sourceFolderId={}, targetFolderId={}, count={}",
                accountId, sourceFolderId, requestForm.getTargetFolderId(), requestForm.getTermIds().size());
        return MoveFolderTermsResponseForm.from(response);
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
        long count = wordbookFolderQueryService.countTermsInFolderOrThrow(accountId, folderId);
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

        var res = wordbookFolderService.list(req);
        return ListWordbookTermResponseForm.from(res);
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
