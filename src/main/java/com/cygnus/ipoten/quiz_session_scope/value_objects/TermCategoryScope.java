package com.cygnus.ipoten.quiz_session_scope.value_objects;

import com.cygnus.ipoten.quiz_set.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz_set.service.request.CreateQuizSetByCategoryRequest;
import lombok.Value;

import java.util.List;

/**
 * 카테고리 기반 퀴즈 세션 생성을 위한 출제 범위(Scope)를 표현하는 Value Object.
 *
 * 이 클래스는 "특정 카테고리에서 어떤 조건으로 몇 문제를 출제할 것인가"라는
 * 퀴즈 세션의 핵심 의도를 하나의 도메인 객체로 응집한다.
 *
 * 단순히 파라미터 묶음 DTO가 아니라,
 * 출제 개수, 문제 유형, 난이도, 시드 정책에 대한
 * 도메인 규칙과 제약을 함께 캡슐화한 범위 객체이다.
 *
 * 구성 요소 의미:
 * - categoryId       : 출제 대상이 되는 문제 카테고리 식별자
 * - count            : 해당 카테고리에서 출제할 문제 수 (1~100)
 * - questionTypeScope: 문제 유형 출제 범위 (단일 유형 또는 MIX)
 * - difficultyScope  : 난이도 출제 범위 (단일 난이도 또는 MIX)
 * - seedPolicy       : 문제 랜덤 순서를 결정하는 시드 정책
 *
 * 책임과 역할:
 * - 카테고리 기반 출제에 필요한 모든 조건을 하나의 VO로 표현
 * - 잘못된 출제 범위(카테고리 없음, 문제 수 초과 등)를 생성 시점에 차단
 * - 하위 도메인(CreateQuizSet 요청)으로 안전하게 변환
 *
 * 설계 의도:
 * - Lombok @Value 기반의 불변(Value Object) 설계
 * - 출제 범위 관련 로직을 서비스 계층이 아닌 도메인 객체로 이동
 * - QuestionTypeScope, DifficultyScope, SeedPolicy와 조합되어
 *   퀴즈 세션 생성 정책을 명확히 드러내는 모델 구성
 *
 * 사용 예:
 * - 특정 카테고리에서 객관식 + MIX 난이도 10문제 출제
 * - 고정 시드 기반으로 재현 가능한 퀴즈 세트 생성
 *
 * 주의 사항:
 * - 이 객체는 "카테고리 출제 스코프"에만 집중하며,
 *   실제 문제 추출 및 섞기 로직은 별도의 생성 서비스에서 수행된다.
 */
@Value
public class TermCategoryScope {
    Long categoryId;
    int count;
    QuestionTypeScope questionTypeScope;
    DifficultyScope difficultyScope;
    List<String> topicTagKeys;

    public TermCategoryScope(Long categoryId,
                             int count,
                             QuestionTypeScope questionTypeScope,
                             DifficultyScope difficultyScope,
                             List<String> topicTagKeys) {

        if (categoryId == null) throw new IllegalArgumentException("CategoryScope: categoryId는 필수입니다.");
        if (count <= 0 || count > 100) throw new IllegalArgumentException("CategoryScope: count는 1~100 사이어야 합니다.");

        this.categoryId = categoryId;
        this.count = count;
        this.questionTypeScope = questionTypeScope;
        this.difficultyScope = difficultyScope;

        this.topicTagKeys = (topicTagKeys == null) ? List.of()
                : topicTagKeys.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase())
                .distinct()
                .toList();
    }

    public CreateQuizSetByCategoryRequest toCreateQuizSetRequest(String title) {
        return new CreateQuizSetByCategoryRequest(
                title,
                this.categoryId,
                this.count,
                this.questionTypeScope.getType(),
                this.difficultyScope.getLevel()
        );
    }

    public String getTypeRaw() {
        // resolveTypes()가 lower-case도 처리하니까 "CHOICE/OX/INITIALS/MIX"로 줘도 OK
        return (questionTypeScope == null) ? "MIX" : questionTypeScope.toString();
    }

    public QuizSetType getTypeAsQuizSetType() {
        // QuizSetType.fromParam은 null/blank -> CHOICE로 가버리니까,
        // scope에서 null이면 MIX로 보정하는 게 안전함
        String raw = getTypeRaw();
        if (raw == null || raw.isBlank()) return QuizSetType.MIX;
        return QuizSetType.fromParam(raw);
    }
}
