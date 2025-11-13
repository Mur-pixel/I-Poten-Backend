package com.cygnus.ipoten.quiz.controller.response_form;

import lombok.Builder;
import lombok.Value;
import java.util.List;

@Value
@Builder
public class QuizTrendResponseForm {
    String metric;          // accuracy | sets | retryRate
    String span;            // 기간
    List<Point> points;     // daily series(yyyy-MM-dd asc)

    @Value
    @Builder
    public static class Point{
        String date;        // yyyy-MM-dd (KST)
        double value;       // accuracy/retryRate: 0~100, 점수(double로 전달)
    }
}
