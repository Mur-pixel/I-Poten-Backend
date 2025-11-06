package com.cygnus.ipoten.userTrustscore.controller.response;

import com.cygnus.ipoten.userTrustscore.entity.TrustScore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class TrustScoreResponse {

    private double attendanceRate;

    private double attendanceScore;
    private double interviewScore;
    private double problemScore;
    private double studyroomScore;

    private double totalScore;
    private LocalDateTime calculatedAt;

    public static TrustScoreResponse fromEntity(TrustScore entity) {
        return new TrustScoreResponse(
                entity.getAttendanceRate(),
                entity.getAttendanceScore(),
                entity.getInterviewScore(),
                entity.getProblemScore(),
                entity.getStudyroomScore(),
                entity.getTotalScore(),
                entity.getCalculatedAt()
        );
    }
}