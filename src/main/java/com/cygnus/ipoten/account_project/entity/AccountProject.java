package com.cygnus.ipoten.account_project.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.cygnus.ipoten.account.entity.Account;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
public class AccountProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(name = "project_description", nullable = false)
    private String projectDescription;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    @CreationTimestamp
    private LocalDateTime creationAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    public AccountProject(Account account, String projectName, String projectDescription) {
        this.account = account;
        this.projectName = projectName;
        this.projectDescription = projectDescription;
        this.isActive = true;
    }
}
