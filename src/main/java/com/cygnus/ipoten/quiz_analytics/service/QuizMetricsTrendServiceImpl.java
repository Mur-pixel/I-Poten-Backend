package com.cygnus.ipoten.quiz_analytics.service;

import com.cygnus.ipoten.quiz_analytics.controller.response_form.QuizTrendResponseForm;
import com.cygnus.ipoten.quiz_analytics.repository.QuizMetricsTrendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizMetricsTrendServiceImpl implements QuizMetricsTrendService {

    private final QuizMetricsTrendRepository quizMetricsTrendRepository;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public QuizTrendResponseForm getTrend(Long accountId, String metric, String span) {
        MetricType type = MetricType.from(metric);

        int days = parseDays(span, 30);
        LocalDate todayKst = LocalDate.now(KST);
        LocalDate start = todayKst.minusDays(days - 1);
        Instant from = start.atStartOfDay(KST).toInstant();
        Instant to   = todayKst.atTime(LocalTime.MAX).atZone(KST).toInstant();

        Map<LocalDate, Double> daily = switch (type) {
            case SETS      -> querySets(accountId, from, to);
            case RETRYRATE -> queryRetryRate(accountId, from, to);
            case ACCURACY  -> queryAccuracy(accountId, from, to);
        };

        List<QuizTrendResponseForm.Point> points = new ArrayList<>(days);
        for (LocalDate d = start; !d.isAfter(todayKst); d = d.plusDays(1)) {
            double v = daily.getOrDefault(d, 0.0);
            v = Math.round(v * 10.0) / 10.0;
            points.add(QuizTrendResponseForm.Point.builder()
                    .date(DF.format(d))
                    .value(v)
                    .build());
        }

        return QuizTrendResponseForm.builder()
                .metric(type.param)
                .span(DF.format(start) + ".." + DF.format(todayKst))
                .points(points)
                .build();
    }

    @Override
    public Map<LocalDate, Double> querySets(Long aid, Instant from, Instant to) {
        Map<LocalDate, Double> map = new LinkedHashMap<>();
        for (Object[] row : quizMetricsTrendRepository.countSubmittedSetsByDay(aid, from, to)) {
            LocalDate day = toLocalDate(row[0]);
            long count = ((Number) row[1]).longValue();
            map.put(day, (double) count);
        }
        return map;
    }

    @Override
    public Map<LocalDate, Double> queryRetryRate(Long aid, Instant from, Instant to) {
        Map<LocalDate, Double> map = new LinkedHashMap<>();
        for (Object[] row : quizMetricsTrendRepository.countSubmittedAndRetryByDay(aid, from, to)) {
            LocalDate day = toLocalDate(row[0]);
            long total = ((Number) row[1]).longValue();
            long retry = ((Number) row[2]).longValue();
            double rate = (total > 0) ? (retry * 100.0 / total) : 0.0;
            map.put(day, rate);
        }
        return map;
    }

    @Override
    public Map<LocalDate, Double> queryAccuracy(Long aid, Instant from, Instant to) {
        Map<LocalDate, Double> map = new LinkedHashMap<>();
        for (Object[] row : quizMetricsTrendRepository.sumCorrectAndTotalAnswersByDay(aid, from, to)) {
            try {
                if (row == null || row.length < 3) continue;
                if (row[0] == null) continue;

                log.debug("[accuracy] row types: day={}, correct={}, total={}",
                        row[0] == null ? null : row[0].getClass().getName(),
                        row[1] == null ? null : row[1].getClass().getName(),
                        row[2] == null ? null : row[2].getClass().getName());

                LocalDate day = toLocalDate(row[0]);

                long correct = (row[1] == null) ? 0L : ((Number) row[1]).longValue();
                long total   = (row[2] == null) ? 0L : ((Number) row[2]).longValue();

                double rate  = (total > 0) ? (correct * 100.0 / total) : 0.0;
                map.put(day, rate);
            } catch (Exception e) {
                 log.warn("[accuracy] row parse failed: {}", Arrays.toString(row), e);
            }
        }
        return map;
    }

    @Override
    public long getTotalSets(Long accountId) {
        return quizMetricsTrendRepository.countSubmittedSets(accountId);
    }

    private static int parseDays(String span, int def) {
        if (span == null || span.isBlank()) return def;
        String s = span.trim().toLowerCase(Locale.ROOT);
        if (s.endsWith("d")) {
            try {
                return Math.max(1, Integer.parseInt(s.substring(0, s.length() - 1)));
            } catch (NumberFormatException ignore) {}
        }
        return def;
    }

    private static LocalDate toLocalDate(Object o) {
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date sd) return sd.toLocalDate();
        if (o instanceof LocalDateTime dt) return dt.toLocalDate();
        if (o instanceof Instant i) return i.atZone(KST).toLocalDate();

        String s = String.valueOf(o).trim();
        if (s.length() >= 10) {
            String ymd = s.substring(0, 10); // "yyyy-MM-dd"만 파싱
            try { return LocalDate.parse(ymd); } catch (Exception ignore) {}
        }
        throw new IllegalArgumentException("Unparsable date value: " + o);
    }

    enum MetricType {
        ACCURACY("accuracy"),
        SETS("sets"),
        RETRYRATE("retryRate");

        final String param;
        MetricType(String p){ this.param = p; }
        static MetricType from(String s){
            if (s == null) throw new IllegalArgumentException("metric is required");
            return switch (s.trim()) {
                case "accuracy" -> ACCURACY;
                case "sets" -> SETS;
                case "retryRate" -> RETRYRATE;
                default -> throw new IllegalArgumentException("unsupported metric: " + s);
            };
        }
    }
}
