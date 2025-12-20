package com.cygnus.ipoten.quiz_session_scope.value_objects;

import com.cygnus.ipoten.quiz_session.entity.enums.SeedMode;
import lombok.Value;

import java.util.Locale;

/**
 * QuizSession 생성 시 사용되는 시드(seed) 결정 정책을 표한하는 Value Object
 *
 * 이 클래스는 퀴즈 세션의 문제 출제 순서 랜덤화에 사용되는
 * SeedMode와 fixedSeed 값을 하나의 불변 객체로 캡슐화함
 *
 * 주요 목적은 컨트롤러나 서비스 계층에서 흩어질 수 있는
 * 시드 관련 분기 로직을 도메인 규칙으로 응집시키는 것
 *
 * 책임과 역할:
 * - SeedMode(AUTO, DAILY, FIXED)와 fixedSeed(Long)의 유효한 조합을 보장
 * - "FIXED인데 fixedSeed가 없음" 같은 것을 이곳에서 막음
 * - 문자열 입력(rawMode)을 안전하게 SeedMode enum으로 변환
 * 
 * 설계 의도:
 * - Lombok @Value를 사용한 불변(Value Object) 설계
 * - 생성 로직을 정적 팩토리 메서드(of)로 제한하여 항상 검증된 상태만 생성
 * - Quiz Session Generator 또는 Scope 도메인에서 재사용 가능
 */

@Value
public class SeedPolicy {
    SeedMode seedMode;
    Long fixedSeed;

    public static SeedPolicy fromRaw(String rawMode, Long fixedSeed) {
        SeedMode mode = resolveMode(rawMode, fixedSeed);

        // FIXED인데 seed 없으면 예외
        if (mode == SeedMode.FIXED && fixedSeed == null) {
            throw new IllegalStateException("SeedMode=FIXED일 때 fixedSeed는 필수입니다.");
        }
        return new SeedPolicy(mode, fixedSeed);
    }

    private static SeedMode resolveMode(String rawMode, Long fixedSeed) {
        if (rawMode != null && !rawMode.isBlank()) {
            try {
                return SeedMode.valueOf(rawMode.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignore) {
            }
        }
        if (fixedSeed != null) return SeedMode.FIXED;
        return SeedMode.AUTO;
    }

    public boolean isDaily() { return seedMode == SeedMode.DAILY; }
    public boolean isFixed() { return seedMode == SeedMode.FIXED; }
    public boolean isAuto() { return seedMode == SeedMode.AUTO; }
}
