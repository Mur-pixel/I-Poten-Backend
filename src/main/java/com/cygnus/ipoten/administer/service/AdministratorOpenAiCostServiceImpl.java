package com.cygnus.ipoten.administer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdministratorOpenAiCostServiceImpl implements AdministratorOpenAiCostService {

    private final RestClient openAiAdminClient;

    // 한 번에 받을 최대 버킷 수
    private static final int MAX_PAGE_SIZE = 31;
    private static final long SECONDS_PER_DAY = 86_400L;

    @Override
    public String getDailyCosts(Instant startInclusiveUtc, Instant endExclusiveUtc) {

        if (startInclusiveUtc == null || endExclusiveUtc == null) {
            throw new IllegalArgumentException("start_time/end_time must not be null (end_time is required & exclusive).");
        }
        long start = startInclusiveUtc.getEpochSecond();
        long end   = endExclusiveUtc.getEpochSecond(); // exclusive
        if (start >= end) {
            throw new IllegalArgumentException("start_time must be earlier than end_time (end is exclusive).");
        }

        long seconds = end - start; // end는 이미 exclusive
        long days = (seconds + SECONDS_PER_DAY - 1) / SECONDS_PER_DAY;
        if (days <= 0) {
            throw new IllegalArgumentException("Range must cover at least one day.");
        }

        int limit = (int) Math.min(Math.max(days, 1), MAX_PAGE_SIZE);

//        log.info("receive startUtc={} endUtc={} days={} limit={}",
//                startInclusiveUtc, endExclusiveUtc, days, limit);

        URI uri = UriComponentsBuilder.fromPath("/v1/organization/costs")
                .queryParam("start_time", start)
                .queryParam("end_time", end)
                .queryParam("limit", limit)
                .encode()
                .build()
                .toUri();

        long t0 = System.nanoTime();
        ResponseEntity<String> res = openAiAdminClient
                .get()
                .uri(uri)
                .retrieve()
                .toEntity(String.class);

        int status = res.getStatusCode().value();
//        long durMs = (System.nanoTime() - t0) / 1_000_000L;
//        int len = (res.getBody() == null ? 0 : res.getBody().length());
//        log.info("op=openai_costs uri={} status={} dur_ms={} body_len={}", uri, status, durMs, len);

        if (status == 429 || status >= 500) {
            throw new RuntimeException("OpenAI billing API error: HTTP " + status);
        }
        if (status >= 400) {
            throw new IllegalArgumentException("OpenAI billing API client error: HTTP " + status + " body=" + res.getBody());
        }

        return res.getBody();
    }
}