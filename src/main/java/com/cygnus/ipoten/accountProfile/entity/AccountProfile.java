package com.cygnus.ipoten.accountProfile.entity;


import com.cygnus.ipoten.account.entity.Account;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "account_profile")
@NoArgsConstructor
public class AccountProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    private Account account;

    @Column(name = "nickname", nullable = false, length = 30)
    private String nickname;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    public AccountProfile(Account account, String nickname, String email) {
        this.account = account;
        this.nickname = nickname;
        this.email = email;
    }

    public void updateProfile(String nickname, String email) {
        this.nickname = nickname;
        this.email = email;
    }


}
