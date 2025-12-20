package com.cygnus.ipoten.quiz_session.controller.response_form;

import com.cygnus.ipoten.quiz_session.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class SessionSummaryResponseForm {
    private final Long sessionId;
    private final SessionStatus status;     // IN_PROGRESS | SUBMITTED | EXPIRED
    private final Integer totalCount;       // 전체 문항 수
    private final Instant lastActivityAt;   // 사용자의 마지막 활동 시각(이 값으로 만료 전환 여부를 판단)
    private final SeedMode seedMode;        // 세션 시드 모드(FIXED(고정 시드(재현 가능)) | DAILY(계정별/일자별 고정) | AUTO(매번 랜덤))
}
