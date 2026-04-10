package com.cygnus.iptn.authentication.social;

import com.cygnus.iptn.account.entity.Account;

// 소셜 로그인 정책 판단 결과를 담는 DTO
// 로그인 시점에 사용자가 신규 회원인지, 재가입 대상인지,
// 또는 바로 로그인 가능한 기존 회원인지를 구분하기 위해 사용
public record SocialLoginResult(

        // 신규 회원 여부
        // true 이면 아직 정식 가입이 완료되지 않은 상태이며
        // 추가 회원가입 절차로 이동해야 함
        boolean isNewUser,

        // 재가입 대상 여부
        // true 이면 과거 탈퇴 이력이 있는 계정이며
        // 재가입 가능 정책을 통과한 상태를 의미
        boolean isRejoinUser,

        // 발급된 토큰
        // 신규/재가입 대상이면 임시 토큰(temp token),
        // 기존 활성 회원이면 실제 사용자 토큰(user token)
        String token,

        // 기존 계정 정보
        // 신규 회원이면 null 일 수 있고,
        // 기존 회원 또는 재가입 대상이면 해당 Account 가 담김
        Account account
) {
}