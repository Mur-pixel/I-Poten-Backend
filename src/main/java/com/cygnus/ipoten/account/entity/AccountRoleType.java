package com.cygnus.ipoten.account.entity;


import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "account_role_type")
public class AccountRoleType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false)
    private RoleType roleType;


    public AccountRoleType(RoleType roleType) {
        this.roleType = roleType;
    }

    public AccountRoleType() {

    }
}
