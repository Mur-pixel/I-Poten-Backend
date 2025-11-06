package com.cygnus.ipoten.account.entity;


import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_type_id", nullable = false)
    private AccountRoleType accountRoleType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "login_type_id", nullable = false)
    private AccountLoginType accountLoginType;

    public Account(Long id) {
        this.id = id;
    }

    public Account() {

    }

    public Account(AccountRoleType accountRoleType, AccountLoginType accountLoginType) {
        this.accountRoleType = accountRoleType;
        this.accountLoginType = accountLoginType;
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
}
