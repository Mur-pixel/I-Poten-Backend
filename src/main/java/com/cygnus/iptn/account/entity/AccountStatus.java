package com.cygnus.iptn.account.entity;

// 계정 상태를 나타내는 Enum
public enum AccountStatus {

    // 정상적으로 서비스 이용 가능한 상태
    // 로그인, API 호출, 모든 기능 사용 가능
    ACTIVE,

    // 회원 탈퇴 상태 (Soft Delete)
    // 로그인 및 서비스 접근 제한됨
    WITHDRAWN
}