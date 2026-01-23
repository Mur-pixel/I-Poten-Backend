package com.cygnus.ipoten.term_trending.service;

import com.cygnus.ipoten.term_trending.repository.TermSearchStatsDailyRepository;
import com.cygnus.ipoten.term_trending.service.request.TrendingTermRequest;
import com.cygnus.ipoten.term_trending.service.response.TrendingTermResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TermTrendingServiceImpl implements TermTrendingService {

    private final TermSearchStatsDailyRepository termSearchStatsDailyRepository;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    @Transactional
    public TrendingTermResponse getTrending(TrendingTermRequest request) {
        DateRange range = DateRange.from(request.range());
        int limit = Math.min(Math.max(request.limit(), 1), 100);

        LocalDate today = LocalDate.now(KST);
        LocalDate fromDate = range.fromDate(today);

        List<TermSearchStatsDailyRepository.TrendingRow> rows =
                termSearchStatsDailyRepository.findTrending(fromDate, today, limit);

        List<TrendingTermResponse.Item> items = rows.stream()
                .map(r -> new TrendingTermResponse.Item(
                        r.getTermId(),
                        r.getTitle(),
                        r.getSearchCount(),
                        r.getLastSearchedAt()
                ))
                .toList();
        return new TrendingTermResponse(request.range(), limit, items);
    }

    enum DateRange {
        H24, D7, D30, ALL;

        static DateRange from(String raw) {
            if (raw == null) return D7;
            return switch (raw.trim().toLowerCase()) {
                case "24h", "1d" -> H24;
                case "7d" -> D7;
                case "30d" -> D30;
                case "all" -> ALL;
                default -> D7;
            };
        }

        LocalDate fromDate(LocalDate today) {
            return switch (this) {
                case H24 -> today.minusDays(1);
                case D7 -> today.minusDays(6);
                case D30 -> today.minusDays(29);
                case ALL -> LocalDate.of(2000, 1, 1);
            };
        }
    }
}
