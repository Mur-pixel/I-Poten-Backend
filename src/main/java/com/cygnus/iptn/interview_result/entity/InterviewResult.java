package com.cygnus.iptn.interview_result.entity;

import com.cygnus.iptn.interview.entity.Interview;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "interview_result")
public class InterviewResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Interview interview;

    @Lob
    @Column(name = "overall_commnet", columnDefinition = "TEXT")
    private String overallComment;

    public InterviewResult(Interview interview, String overallComment) {
        this.interview = interview;
        this.overallComment = overallComment;
    }

    public InterviewResult() {

    }
}
