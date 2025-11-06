package com.cygnus.ipoten.userTrustscore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "trust_score")
public class TrustScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long accountId;

    // 점수 계산 최소 정보 (%)
    @Column(nullable = false, name = "attendance_rate")
    private double attendanceRate; // 이번 달 출석률 (%)

    // 환산 점수
    @Column(nullable = false, name = "attendance_score")
    private double attendanceScore; // 출석 점수 (최대 40)

    @Column(nullable = false, name = "interview_score")
    private double interviewScore; // 면접 점수 (최대 25)

    @Column(nullable = false, name = "problem_score")
    private double problemScore; // 문제 풀이 점수 (최대 25)

    @Column(nullable = false, name = "studyroom_score")
    private double studyroomScore; // 스터디룸 점수 (최대 10)

    //총점 및 시각
    @Column(nullable = false)
    private double totalScore; // 총점 (0~100)

    @Column(nullable = false, name = "calculated_at")
    private LocalDateTime calculatedAt = LocalDateTime.now();

    public void updateScores(double attendanceRate,
                             double attendanceScore,
                             double interviewScore,
                             double problemScore,
                             double studyroomScore,
                             double totalScore) {
        this.attendanceRate = attendanceRate;
        this.attendanceScore = attendanceScore;
        this.interviewScore = interviewScore;
        this.problemScore = problemScore;
        this.studyroomScore = studyroomScore;
        this.totalScore = totalScore;
        this.calculatedAt = LocalDateTime.now();
    }
}

