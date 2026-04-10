package com.cygnus.iptn.interviewQA.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.cygnus.iptn.interview.entity.Interview;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "interview_qa")
public class InterviewQA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @Lob
    @Column(name = "question", columnDefinition = "TEXT")
    private String question;

    @Lob
    @Column(name = "answer", nullable = true, columnDefinition = "TEXT")
    private String answer;

    
    @Column(name = "created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    @CreationTimestamp
    private LocalDateTime createdAt;

    public InterviewQA(Interview interview, String question, String answer) {
        this.interview = interview;
        this.question = question;
        this.answer = answer;
    }

    public InterviewQA(Interview interview, String question) {
        this.interview = interview;
        this.question = question;
    }

    public InterviewQA(String question) {
        this.question = question;
    }

    public InterviewQA(Interview interview) {
        this.interview = interview;
    }

    public InterviewQA() {
    }
}
