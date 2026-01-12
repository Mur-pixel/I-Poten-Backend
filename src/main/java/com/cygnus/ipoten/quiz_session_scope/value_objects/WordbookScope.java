package com.cygnus.ipoten.quiz_session_scope.value_objects;

import com.cygnus.ipoten.quiz_set.service.request.CreateQuizSetByWordbookRequest;
import lombok.Value;

/**
 * 사용자 단어장(Wordbook) 기반 퀴즈 세션 생성을 위한 출제 범위를 표현하는 Value Object.
 *
 * 이 클래스는 "특정 사용자의 단어장에서 어떤 조건으로 몇 문제를 출제할 것인가"라는
 * 퀴즈 세션 생성 의도를 하나의 도메인 객체로 응집한다.
 *
 * 카테고리나 직무처럼 공용 기준이 아닌,
 * 사용자 소유의 단어장(User Wordbook)을 출제 기준으로 삼는다는 점에서
 * 다른 Scope(CategoryScope, JobScope)와 명확히 구분된다.
 *
 * 구성 요소 의미:
 * - accountId         : 단어장 소유자 식별자 (사용자 기준 출제 보장)
 * - wordbookId        : 출제 대상이 되는 단어장 식별자
 * - count             : 단어장에서 출제할 문제 수 (1~100)
 * - questionTypeScope : 문제 유형 출제 범위 (단일 유형 또는 MIX)
 * - difficultyScope   : 난이도 출제 범위 (단일 난이도 또는 MIX)
 * - seedPolicy        : 문제 랜덤 순서 및 재현성을 결정하는 시드 정책
 *
 * 책임과 역할:
 * - 사용자 단어장 기반 출제에 필요한 모든 조건을 하나의 VO로 표현
 * - 잘못된 출제 범위(accountId/wordbookId 누락, 문제 수 초과 등)를 생성 시점에 차단
 * - 퀴즈 세션 생성에 필요한 요청 객체(CreateQuizSetByWordbookRequest)로 안전하게 변환
 *
 * 설계 의도:
 * - Lombok @Value 기반의 불변(Value Object) 설계
 * - "출제 기준(Wordbook)"과 "출제 조건(Scope)"을 명확히 분리
 * - QuestionTypeScope, DifficultyScope, SeedPolicy와 조합되어
 *   사용자 맞춤 학습 세션의 출제 정책을 명확히 드러냄
 *
 * 사용 예:
 * - 특정 사용자의 단어장에서 MIX 유형 + EASY 난이도 15문제 출제
 * - 고정 시드를 사용해 동일한 단어장 퀴즈 세션을 반복 생성
 *
 * 주의 사항:
 * - 이 객체는 단어장 기반 출제 범위 정의에만 집중하며,
 *   실제 문제 추출, 정렬, 섞기 로직은 별도의 퀴즈 세션 생성 서비스에서 수행된다.
 */
@Value
public class WordbookScope {
    Long accountId;
    Long wordbookId;
    int count;
    QuestionTypeScope questionTypeScope;
    DifficultyScope difficultyScope;

    public WordbookScope(Long accountId,
                         Long wordbookId,
                         int count,
                         QuestionTypeScope questionTypeScope,
                         DifficultyScope difficultyScope
    ) {
        if (accountId == null || wordbookId == null) {
            throw new IllegalArgumentException("WordbookScope: accountId/wordbookId는 필수입니다.");
        }
        if (count<=0 || count > 100) {
            throw new IllegalArgumentException("WordbookScope: count는 1~100 사이여야 합니다.");
        }
        this.accountId = accountId;
        this.wordbookId = wordbookId;
        this.count = count;
        this.questionTypeScope = questionTypeScope;
        this.difficultyScope = difficultyScope;
    }

    public CreateQuizSetByWordbookRequest toCreateQuizSetRequest(String title) {
        return new CreateQuizSetByWordbookRequest(
                accountId,
                wordbookId,
                count,
                CreateQuizSetByWordbookRequest.QuestionType.valueOf(
                        questionTypeScope.getType().name()
                ),
                difficultyScope.toDifficultyLevel(),
                (title == null || title.isBlank()) ? null : title.trim()
        );
    }
}
