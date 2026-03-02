package com.cygnus.ipoten.fcm.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "account_fcm_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountFcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, unique = true)
    private Long accountId;

    @Column(nullable = false, length = 512)
    private String token;

    public AccountFcmToken(Long accountId, String token) {
        this.accountId = accountId;
        this.token = token;
    }

    public void updateToken(String token) {
        this.token = token;
    }
}
