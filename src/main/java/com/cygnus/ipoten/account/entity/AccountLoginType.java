package com.cygnus.ipoten.account.entity;


import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "account_login_type")
public class AccountLoginType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", nullable = false, length = 20)
    private LoginType loginType;

    public AccountLoginType(LoginType loginType) {
        this.loginType = loginType;
    }

    public AccountLoginType() {

    }




}
