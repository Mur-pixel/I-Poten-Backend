package com.cygnus.ipoten.wordbook_pdf.entity;

import com.cygnus.ipoten.account.entity.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "wordbook_pdf")
public class WordbookPdf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "title", nullable = false)
    private String title;

    private LocalDateTime createdAt;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String termIdsJson;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

}
