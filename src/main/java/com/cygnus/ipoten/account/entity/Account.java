package com.cygnus.ipoten.account.entity;


import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Getter
@Table(name = "account")
public class Account {

    // 재가입 대기 기간 (탈퇴 후 30일 지나야 재가입 가능)
    // 서비스 정책에 따라 변경될 수 있는 핵심 비즈니스 상수
    public static final long REJOIN_WAIT_DAYS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_type_id", nullable = false)
    private AccountRoleType accountRoleType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "login_type_id", nullable = false)
    private AccountLoginType accountLoginType;

    // 계정 상태 (ACTIVE, WITHDRAWN 등)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    // 계정 생성 시각 (최초 가입 시 기록, 수정 불가)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // 탈퇴 시각 (탈퇴한 경우에만 값 존재)
    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    public Account(Long id) {
        this.id = id;
    }

    public Account() {

    }

    // 회원가입 시 사용하는 생성자
    // 기본 상태는 ACTIVE, 탈퇴일은 null
    public Account(AccountRoleType accountRoleType, AccountLoginType accountLoginType) {
        this.accountRoleType = accountRoleType;
        this.accountLoginType = accountLoginType;
        this.status = AccountStatus.ACTIVE;
        this.withdrawnAt = null;
    }
    //로그인 타입 교체(검증 포함) <- 2025.09.14 발키리 추가
    public void changeLoginType(AccountLoginType newType) {
        if (newType == null) throw new IllegalArgumentException("loginType null");
        if (this.accountLoginType == newType) return;
        this.accountLoginType = newType;
    }

    //관리자 권한 부여(역할 검증 포함) <- 2025.09.14 발키리 추가
    public void grantAdmin(AccountRoleType adminRole) {
        if (adminRole == null || adminRole.getRoleType() != RoleType.ADMIN)
            throw new IllegalArgumentException("admin role required");
        this.accountRoleType = adminRole;
    }

    // 회원 탈퇴 시 삭제가 아닌 상태값 변경 + 탈퇴 시각 기록 (임시)
    public void markWithdrawn() {
        this.status = AccountStatus.WITHDRAWN;
        this.withdrawnAt = Instant.now();
    }

    // 계정 재활성화 (복구)
    // 탈퇴 상태를 ACTIVE로 변경하고 탈퇴 시간 초기화
    public void reactivate() {
        this.status = AccountStatus.ACTIVE;
        this.withdrawnAt = null;
    }

    // 현재 계정이 탈퇴 상태인지 여부
    public boolean isWithdrawn() {
        return this.status == AccountStatus.WITHDRAWN;
    }

    // 재가입 가능 여부 판단
    // 탈퇴 상태 + 탈퇴일 기준으로 REJOIN_WAIT_DAYS 이후인지 확인
    public boolean canRejoinAt(Instant now) {
        if (!isWithdrawn() || withdrawnAt == null) {
            return false;
        }
        return !now.isBefore(withdrawnAt.plus(REJOIN_WAIT_DAYS, ChronoUnit.DAYS));
    }

    // 재가입 가능 시점 반환
    // UI에서 "몇 일 후 재가입 가능" 같은 안내에 활용
    public Instant getRejoinAvailableAt() {
        return withdrawnAt == null ? null : withdrawnAt.plus(REJOIN_WAIT_DAYS, ChronoUnit.DAYS);
    }

    // 활성 계정인지 검증
    // 탈퇴 계정 접근 시 예외 발생 (보안 및 데이터 무결성 중요)
    public void ensureActive() {
        if (isWithdrawn()) {
            throw new IllegalStateException("탈퇴한 계정입니다.");
        }
    }

    // DB insert 전에 자동 실행
    // 상태값과 생성일을 기본값으로 세팅 (null 방지)
    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = AccountStatus.ACTIVE;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
