package com.cygnus.ipoten.custom_term_admin;

import com.cygnus.ipoten.custom_term_admin.service.CustomTermEraseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/internal/admin/customterm")
public class CustomTermAdminController {

    private final CustomTermEraseService customTermEraseService;

    /**
     * 내부(Admin) 호출용: custom_term 도메인 데이터만 계정 기준으로 삭제
     * - wordbook_folder / wordbook_term / learning_progress
     *   등 계정과 직접적으로 연결된 단어장/진행도/최근 용어 데이터를 일괄 정리한다.
     * - quiz 도메인과는 별도 엔드포인트에서 관리.
     */
    @Operation(
            summary = "[내부] 특정 계정의 user_term 데이터 일괄 삭제",
            description = "운영/배치용. 단어장/진행도/최근 용어 등 user_term 영역 전체를 accountId 기준으로 정리합니다."
    )
    @DeleteMapping("/internal/admin/accounts/{accountId}/user-term:erase")
    public ResponseEntity<?> eraseUserTermByAccount(
            @Parameter(description = "정리 대상 계정 ID", example = "1")
            @PathVariable Long accountId) {
        var result = customTermEraseService.eraseByAccountId(accountId);

        // 운영/모니터링 편의를 위한 요약 응답 본문 구성
        Map<String, Object> body = Map.of(
                "accountId", accountId,
                "deleted", Map.of(
                        // 이번 호출에서 "실제로 지워진" 행 수 (선삭제 + FK 제약 충돌 방지 포함)
                        "wordbook_folder", result.getFolders(),
                        "wordbook_term",   result.getWordbookTerms(),
                        "learning_progress",   result.getProgresses()
                )
        );

        log.info("[custom-term:erase] {}", body);
        return ResponseEntity.ok(body);
    }
}
