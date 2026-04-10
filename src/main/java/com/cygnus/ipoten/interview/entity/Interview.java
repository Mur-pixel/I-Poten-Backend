package com.cygnus.ipoten.interview.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.cygnus.ipoten.interviewee_profile.entity.IntervieweeProfile;
import com.cygnus.ipoten.account.entity.Account;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "interview")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    private boolean isFinished;

    private InterviewPlan plan;

    private CandidateStatus candidateStatus;

    private String selfConcern;

    private String sender;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewee_profile_id")
    private IntervieweeProfile intervieweeProfile;

    @Column(name = "interview_sequence")
    private int interviewSequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type")
    private InterviewType interviewType;

    @Column(name = "created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    private LocalDateTime deletedAt;


    public Interview(Account account, IntervieweeProfile intervieweeProfile, InterviewType interviewType) {
        this.account = account;
        this.intervieweeProfile = intervieweeProfile;
        this.createdAt = LocalDateTime.now();
        this.isFinished = false;
        this.interviewType = interviewType;
        this.interviewSequence = 1;
    }

    public Interview(Account account, IntervieweeProfile intervieweeProfile, InterviewType interviewType, InterviewPlan plan) {
        this.account = account;
        this.intervieweeProfile = intervieweeProfile;
        this.createdAt = LocalDateTime.now();
        this.isFinished = false;
        this.interviewType = interviewType;
        this.interviewSequence = 1;
        this.plan = plan;
    }


    public Interview() {

    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }
}
