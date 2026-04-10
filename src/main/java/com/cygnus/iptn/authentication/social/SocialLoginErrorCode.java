package com.cygnus.iptn.authentication.social;

// 소셜 로그인 과정에서 발생할 수 있는 에러 코드를 정의하는 Enum
public enum SocialLoginErrorCode {

    // 탈퇴한 계정으로 로그인 또는 재가입 제한 기간 중 로그인 시 사용하는 에러 코드
    // 클라이언트에서는 이 값을 기준으로
    // "탈퇴한 계정입니다", "재가입 가능 시점을 확인해 주세요" 같은 분기 처리를 할 수 있음
    ACCOUNT_WITHDRAWN
}