package com.cygnus.iptn.quiz_session_scope.value_objects;

import com.cygnus.iptn.quiz_session.entity.enums.SessionSourceType;
import lombok.Value;

/**
 * 퀴즈 세션 생성 시 적용될 출제 조건(Scope Condition)을 표현하는 최상위 Value Object.
 *
 * 이 클래스는 퀴즈 세션이 어떤 기준(SourceType)을 통해 생성되는지와,
 * 그 기준에 따라 사용될 구체적인 출제 범위(Scope)를 하나의 객체로 캡슐화한다.
 *
 * 즉, "어디에서 문제를 가져올 것인가"와
 * "그 범위 안에서 어떤 조건으로 문제를 출제할 것인가"를
 * 동시에 표현하는 도메인 모델이다.
 *
 * SourceType 의미:
 * - WORDBOOK      : 사용자의 단어장을 기준으로 퀴즈 세션 생성
 * - TERM_CATEGORY : 특정 문제 카테고리를 기준으로 퀴즈 세션 생성
 * - SET           : 이미 정의된 퀴즈 세트를 기준으로 퀴즈 세션 생성
 *
 * 구성 요소 의미:
 * - sourceType    : 퀴즈 세션 생성의 기준이 되는 출처 타입
 * - wordbookScope : 단어장 기반 출제 범위 (sourceType=WORDBOOK)
 * - termCategoryScope  : 카테고리 기반 출제 범위 (sourceType=TERM_CATEGORY)
 * - setScope           : 세트 기반 출제 범위 (sourceType=SET)
 * - seedPolicy    : 출제 순서 및 랜덤성을 결정하는 공통 시드 정책
 *
 * 책임과 역할:
 * - 서로 다른 출제 기준을 단일한 인터페이스(ScopeCondition)로 통합
 * - sourceType에 따라 유효한 Scope 조합만 생성되도록 팩토리 메서드 제공
 * - 퀴즈 세션 생성 로직에서 분기(if/else, switch)의 복잡도를 낮춤
 *
 * 설계 의도:
 * - Lombok @Value 기반의 불변(Value Object) 설계
 * - 출제 기준(Source)과 출제 범위(Scope)를 명확히 분리하면서도 하나의 맥락으로 묶음
 * - TermCategoryScope, WordbookScope 등 하위 Scope들의
 *   진입 지점을 단일 객체로 통합하여 도메인 흐름을 단순화
 *
 */
@Value
public class ScopeCondition {

    public enum SourceType {
        WORDBOOK, TERM_CATEGORY, SET, WRONG_NOTE, LABELS
    }

    SourceType sourceType;

    // sourceType에 따라 하나만 채워지는 스코프들
    WordbookScope wordbookScope;
    TermCategoryScope termCategoryScope;

    // SET 전용
    SetScope setScope;

    // WRONG_NOTE 전용
    WrongNoteScope wrongNoteScope;

    // Label 전용
    LabelsScope labelsScope;

    // 공통 시드 정책
    SeedPolicy seedPolicy;

    String customTitle;

    public static ScopeCondition forWordbook(WordbookScope wordbookScope, SeedPolicy seedPolicy) {
        return forWordbook(wordbookScope, seedPolicy, null);
    }

    public static ScopeCondition forWordbook(WordbookScope wordbookScope, SeedPolicy seedPolicy, String customTitle) {
        return new ScopeCondition(
                SourceType.WORDBOOK,
                wordbookScope,
                null,
                null,
                null,
                null,
                seedPolicy,
                customTitle
        );
    }

    public static ScopeCondition forCategory(TermCategoryScope termCategoryScope, SeedPolicy seedPolicy) {
        return forCategory(termCategoryScope, seedPolicy, null);
    }

    public static ScopeCondition forCategory(TermCategoryScope termCategoryScope, SeedPolicy seedPolicy, String customTitle) {
        return new ScopeCondition(
                SourceType.TERM_CATEGORY,
                null,
                termCategoryScope,
                null,
                null,
                null,
                seedPolicy,
                customTitle
        );
    }

    public static ScopeCondition forSet(SetScope setScope, SeedPolicy seedPolicy) {
        return forSet(setScope, seedPolicy, null);
    }

    public static ScopeCondition forSet(SetScope setScope, SeedPolicy seedPolicy, String customTitle) {
        return new ScopeCondition(
                SourceType.SET,
                null,
                null,
                setScope,
                null,
                null,
                seedPolicy,
                customTitle
        );
    }

    public static ScopeCondition forWrongNote(WrongNoteScope wrongNoteScope, SeedPolicy seedPolicy) {
        return forWrongNote(wrongNoteScope, seedPolicy, null);
    }

    public static ScopeCondition forWrongNote(WrongNoteScope wrongNoteScope, SeedPolicy seedPolicy, String customTitle) {
        return new ScopeCondition(
                SourceType.WRONG_NOTE,
                null,
                null,
                null,
                wrongNoteScope,
                null,
                seedPolicy,
                customTitle
        );
    }

    public static ScopeCondition forLabels(LabelsScope scope, SeedPolicy seedPolicy, String customTitle) {
        return new ScopeCondition(
                SourceType.LABELS,
                null,
                null,
                null,
                null,
                scope,
                seedPolicy,
                customTitle
        );
    }
}
