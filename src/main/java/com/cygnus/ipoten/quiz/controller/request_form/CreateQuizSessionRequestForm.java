package com.cygnus.ipoten.quiz.controller.request_form;

import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz.service.request.CreateQuizSessionRequest;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
/**
 * 즐겨찾기(선택적으로 폴더 지정) 기반 퀴즈 세션 생성 요청 폼.
 *
 * - 출제 개수 지정 방식은 두 가지 중 하나만 사용:
 *   1) count (전체 문항 수)
 *   2) each (문항 유형별 개수: mcqEach/oxEach/initialsEach)
 *
 * - seedMode가 FIXED면 fixedSeed가 필수.
 * - seedMode가 null이면 AUTO로 처리.
 * - difficulty가 null이면 MEDIUM으로 처리(서비스에서 enum 변환 시 기본값 사용).
 * - folderId가 null이면 "전체 즐겨찾기"에서 출제, 값이 있으면 소유권 검증 후 해당 폴더만 사용.
 */
@Getter
@RequiredArgsConstructor
public class CreateQuizSessionRequestForm {

    /** 최소 1개 이상의 문제 유형이 필요(CHOICE/OX/INITIALS) */
    @NotEmpty
    @Size(min = 1, message = "최소 1개 이상의 문제 유형을 지정하세요.")
    private final List<QuestionType> questionTypes;

    /** 방식 2: 유형별 문항 수 (방식 1과 동시 사용 불가) */
    @Min(1) private final Integer count;
    @Min(0) private final Integer mcqEach;
    @Min(0) private final Integer oxEach;
    @Min(0) private final Integer initialsEach;

    /** 시드 모드: AUTO | DAILY | FIXED (null이면 AUTO로 보정) */
    private final SeedMode seedMode;
    private final Long fixedSeed;       // FIXED일 때만 사용

    /** 난이도 문자열: EASY | MEDIUM | HARD (null이면 MEDIUM으로 보정) */
    private final String difficulty;

    /** 폴더 ID: null이면 전체 즐겨찾기, 값이 있으면 해당 폴더만 사용 */
    private final Long folderId;

    // ===== 유효성 검증 =====

    /** count 방식과 each 방식은 동시에 사용할 수 없음 */
    @AssertTrue(message = "count 또는 각 유형별 개수(each) 방식 중 하나만 사용하세요.")
    private boolean isMutuallyExclusiveCountMode() {
        boolean usingCount = count != null;
        boolean usingEach = safe(mcqEach) + safe(oxEach) + safe(initialsEach) > 0;
        // 둘 다 쓰거나(금지) 둘 다 안 쓰면(금지) false
        return usingCount ^ usingEach;
    }

    /** each 방식을 쓴다면 합계가 최소 1 이상이어야 함 */
    @AssertTrue(message = "유형별(each) 합계는 최소 1개 이상이어야 합니다.")
    private boolean isEachSumPositiveIfUsed() {
        boolean usingEach = safe(mcqEach) + safe(oxEach) + safe(initialsEach) > 0;
        if (!usingEach) return true; // each 안 쓰면 통과
        return safe(mcqEach) +  safe(oxEach) + safe(initialsEach) >= 1;
    }

    /** FIXED 모드면 fixedSeed 필수 */
    @AssertTrue(message = "FIXED 모드에서는 fixedSeed가 필요합니다.")
    private boolean isFixedSeedRequiredIfFixedMode() {
        return getNormalizedSeedMode() != SeedMode.FIXED || fixedSeed != null;
    }

    private int safe(Integer v) {
        return v == null ? 0 : v;
    }

    // ===== 기본값 보정 =====

    /** seedMode null → AUTO */
    private SeedMode getNormalizedSeedMode() {
        return seedMode != null ? seedMode : SeedMode.AUTO;
    }

    /** difficulty null → "MEDIUM" (문자열로 보정; 서비스에서 enum 변환/검증) */
    private String getNormalizedDifficulty() {
        return (difficulty == null || difficulty.isBlank()) ? "MEDIUM" : difficulty;
    }

    /** 최근 회피 기간(일). null이면 서버 기본(30일) 사용 */
    @Min(0)
    private final Integer avoidRecentDays;

    // ===== 서비스 요청 변환 =====

    public CreateQuizSessionRequest toServiceRequest(@NotNull Long accountId) {
        return CreateQuizSessionRequest.builder()
                .accountId(accountId)
                .questionTypes(questionTypes)
                .count(count)
                .mcqEach(mcqEach)
                .oxEach(oxEach)
                .initialsEach(initialsEach)
                .seedMode(getNormalizedSeedMode())
                .fixedSeed(fixedSeed)
                .difficulty(getNormalizedDifficulty())
                .folderId(folderId)
                .avoidRecentDays(avoidRecentDays == null ? 30 : avoidRecentDays)
                .build();
    }
}