package com.cygnus.ipoten.wordbook.controller;

import com.cygnus.ipoten.redis_cache.RedisCacheService;
import com.cygnus.ipoten.wordbook.controller.request_form.BulkDeleteWordbookRequestForm;
import com.cygnus.ipoten.wordbook.controller.request_form.CreateWordbookRequestForm;
import com.cygnus.ipoten.wordbook.controller.request_form.RenameWordbookRequestForm;
import com.cygnus.ipoten.wordbook.controller.request_form.ReorderWordbookRequestForm;
import com.cygnus.ipoten.wordbook.controller.response_form.CreateWordbookResponseForm;
import com.cygnus.ipoten.wordbook.controller.response_form.RenameWordbookResponseForm;
import com.cygnus.ipoten.wordbook.repository.WordbookRepository;
import com.cygnus.ipoten.wordbook.service.WordbookQueryService;
import com.cygnus.ipoten.wordbook.service.WordbookService;
import com.cygnus.ipoten.wordbook.service.request.CreateWordbookRequest;
import com.cygnus.ipoten.wordbook.service.response.CreateWordbookResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

// Swagger / OpenAPI
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
    private final RedisCacheService redisCacheService;
    private final WordbookQueryService wordbookQueryService;

    // 단어장 폴더 추가
    @Operation(
            summary = "단어장 폴더 생성",
            description = "현재 로그인한 사용자 기준으로 새로운 단어장 폴더를 생성합니다."
    )
    @PostMapping("/me/folders")
    public CreateWordbookResponseForm createFolder(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody @Valid CreateWordbookRequestForm requestForm) {

        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("[wordbook:create] 인증 실패");
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }

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

    // 단어장 폴더 순서 변경하기
    @Operation(
            summary = "단어장 폴더 순서 재정렬",
            description = "드래그 앤 드롭 등으로 변경된 폴더 정렬 순서를 서버에 반영합니다."
    )
    @PatchMapping("/me/folders:reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderFolders(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody @Valid ReorderWordbookRequestForm requestForm
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("[wordbook:reorder] 인증 실패");
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }
        wordbookService.reorder(requestForm.toRequest(accountId));
    }

    // 단어장 폴더 리스트 조회하기 (단순 목록)
    @Operation(
            summary = "단어장 폴더 목록 조회(간단형)",
            description = "로그인한 사용자의 모든 단어장 폴더 목록(id, 이름, sortOrder)을 조회합니다."
    )
    @GetMapping({"/me/folders", "/user-terms/folders"})
    public List<Map<String, Object>> listFolders(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("[wordbook:list] 인증 실패");
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }
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

    // 단어장 폴더 이름 변경하기
    @Operation(
            summary = "단어장 폴더 이름 변경",
            description = "지정한 폴더 ID의 이름을 수정합니다."
    )
    @PatchMapping("/me/folders/{wordbookId}")
    public RenameWordbookResponseForm renameFolder(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long wordbookId,
            @RequestBody @Valid RenameWordbookRequestForm requestForm) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("[wordbook:attach] 인증 실패");
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }
        log.debug("[wordbook:rename] wordbookId={}", wordbookId);

        var request = requestForm.toRequest(accountId, wordbookId);
        var response = wordbookService.rename(request);
        log.debug("[wordbook:rename] response={}", response);
        return RenameWordbookResponseForm.from(response);
    }

    // 단어장 폴더 삭제(단건)
    @Operation(
            summary = "단어장 폴더 삭제(단건)",
            description = "모드(purge/move)에 따라 폴더 삭제 시 단어 삭제 또는 다른 폴더로 이동 후 삭제를 수행합니다."
    )
    @DeleteMapping("/me/folders/{wordbookId}")
    public ResponseEntity<Void> deleteFolder(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long wordbookId,
            @RequestParam(name = "mode", defaultValue = "purge") String mode,
            @RequestParam(name = "targetWordbookId", required = false) Long targetWordbookId
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("[wordbook:attach] 인증 실패");
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }
        wordbookService.deleteOne(
                accountId,
                WordbookService.DeleteMode.of(mode),
                wordbookId,
                targetWordbookId
        );
        return ResponseEntity.noContent().build();
    }

    // 단어장 폴더 삭제(다건)
    @Operation(
            summary = "단어장 폴더 삭제(다건)",
            description = "여러 개의 폴더를 한 번에 삭제합니다. 모드(purge/move)와 targetWordbookId 사용 패턴은 단건 삭제와 동일합니다."
    )
    @DeleteMapping("/me/folders:bulk")
    public ResponseEntity<Void> deleteFoldersBulk(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestParam(name = "mode", defaultValue = "purge") String mode,
            @RequestParam(name = "targetWordbookId", required = false) Long targetWordbookId,
            @RequestBody @Valid BulkDeleteWordbookRequestForm form
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("[wordbook:attach] 인증 실패");
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }

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

    // PDF 생성을 위해 단어장 폴더의 termId 한 번에 조회하기
    @Operation(
            summary = "단어장 폴더 내 termId 전체 조회(PDF용)",
            description = "E-Book/PDF 생성을 위해, 폴더에 포함된 모든 termId를 한 번에 조회합니다. 개수 제한을 초과하면 413을 반환합니다."
    )
    @GetMapping("/me/folders/{wordbookId}/term-ids")
    public ResponseEntity<?> getAllTermIds(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long wordbookId
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("[wordbook:attach] 인증 실패");
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }

        var result = wordbookService.getAllTermIds(accountId, wordbookId);

        if (result.limitExceeded()) {
            return ResponseEntity.status(PAYLOAD_TOO_LARGE)
                    .header("Ebook-Error", "LIMIT_EXCEEDED")
                    .header("Ebook-Limit", String.valueOf(result.limit()))
                    .header("Ebook-Total", String.valueOf(result.total()))
                    .body("LIMIT_EXCEEDED");
        }
        if (result.termIds().isEmpty()) {
            return ResponseEntity.status(UNPROCESSABLE_ENTITY)
                    .header("Ebook-Error", "EMPTY_WORDBOOK")
                    .body("EMPTY_WORDBOOK");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("wordbookId", result.wordbookId());
        body.put("count", result.termIds().size());
        body.put("termIds", result.termIds());
        return ResponseEntity.ok(body);
    }

    // 내 단어장 폴더 목록과 각 폴더의 즐겨찾기 용어 수 조회하기
    @Operation(
            summary = "단어장 폴더별 즐겨찾기 단어 수 조회",
            description = "내 단어장 폴더 목록과 각 폴더별 즐겨찾기 단어 개수를 함께 조회합니다."
    )
    @GetMapping("/me/wordbook/folders")
    public ResponseEntity<?> getMyFolders(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 실패: 계정 식별 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return ResponseEntity.ok(wordbookQueryService.getMyFolders(accountId));
        } catch (Exception e) {
            log.error("폴더 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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