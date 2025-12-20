package com.cygnus.ipoten.quiz_session_scope.value_objects;

import com.cygnus.ipoten.quiz_set.service.request.CreateQuizSetByCategoryRequest;
import lombok.Value;

/**
 * 퀴즈 세션에서 사용할 문제 유형 범위를 표현하는 Value Object.
 *
 * 이 클래스는 문제 유형(객관식, OX, 초성 등)을 나타내는 값을
 * 그대로 노출하지 않고, 퀴즈 출제 관점에서의 해석과 판단 로직을
 * 도메인 규칙으로 캡슐화한다.
 *
 * 도메인 의미:
 * - 단일 유형(CHOICE, OX, INITIALS 등)은 특정 문제 유형만 출제함을 의미한다.
 * - MIX는 여러 문제 유형을 혼합하여 출제하는 범위 정책을 의미한다.
 *
 * 책임과 역할:
 * - 외부 입력(String raw)을 문제 유형 enum으로 변환하여 표현
 * - 문제 유형이 혼합 출제(MIX)인지 여부를 도메인 메서드로 제공
 * - 서비스 계층에서 문제 유형 분기 로직을 제거하고 의미 중심 판단을 가능하게 함
 *
 * 설계 의도:
 * - Lombok @Value 기반의 불변(Value Object) 설계
 * - 문제 유형의 "종류(type)"와 출제 "범위(scope)"를 구분하여 모델링
 */
@Value
public class QuestionTypeScope {
    CreateQuizSetByCategoryRequest.QuestionType type;

    public static QuestionTypeScope fromRaw(String raw) {
        return new QuestionTypeScope(CreateQuizSetByCategoryRequest.QuestionType.from(raw));
    }

    public boolean isMix(){
        return type == CreateQuizSetByCategoryRequest.QuestionType.MIX;
    }

    public CreateQuizSetByCategoryRequest.QuestionType forCategory() {
        return type;
    }
}
