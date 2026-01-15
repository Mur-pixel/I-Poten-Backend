package com.cygnus.ipoten.wordbook_admin;

import com.cygnus.ipoten.wordbook_admin.service.WordbookAdminEraseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "WordbookAdmin", description = "단어장(Wordbook) 도메인 데이터 정리(회원탈퇴/운영 배치용)")
public class WordbookAdminController {

    private final WordbookAdminEraseService wordbookAdminEraseService;

    /**
     * [내부(Admin/배치) 호출용]
     * 특정 accountId에 연결된 Wordbook 도메인 데이터를 정리합니다.
     *
     * 정리 대상(전부 account 기준 '개인 데이터'):
     * - learning_progress (학습 진행)
     * - wordbook_pdf      (PDF 생성 결과/스냅샷)
     * - wordbook_term     (단어장에 담은 용어 매핑)
     * - wordbook          (단어장)
     *
     * 주의:
     * - term(용어 마스터), quiz_set 같은 "공유 콘텐츠"는 절대 삭제하지 않습니다.
     * - 이 API는 "회원탈퇴 오케스트레이터"에서 호출해도 안전하도록
     *   오직 account 스코프 데이터만 정리합니다.
     */
    @Operation(
            summary = "[내부] 특정 계정의 Wordbook 도메인 데이터 일괄 삭제",
            description = "운영/배치/회원탈퇴 오케스트레이션용. accountId 기준으로 학습진행/단어장/PDF를 정리합니다."
    )
    @DeleteMapping("/internal/admin/accounts/{accountId}/wordbook:erase")
    public ResponseEntity<?> eraseWordbookByAccount(
            @Parameter(description = "정리 대상 계정 ID", example = "1")
            @PathVariable Long accountId
    ) {
        if (accountId == null || accountId <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "accountId must be positive"));
        }

        var result = wordbookAdminEraseService.eraseByAccountId(accountId);

        Map<String, Object> body = Map.of(
                "accountId", accountId,
                "deleted", Map.of(
                        "learning_progress", result.getLearningProgresses(),
                        "wordbook_pdf", result.getWordbookPdfs(),
                        "wordbook_term", result.getWordbookTerms(),
                        "wordbook", result.getWordbooks()
                )
        );

        log.info("[wordbook:erase] {}", body);
        return ResponseEntity.ok(body);
    }
}