package com.cygnus.ipoten.awsCost.service;

import com.cygnus.ipoten.awsCost.entity.AwsDailyCost;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.costexplorer.model.DateInterval;
import software.amazon.awssdk.services.costexplorer.model.GetCostAndUsageRequest;
import software.amazon.awssdk.services.costexplorer.model.GetCostAndUsageResponse;
import software.amazon.awssdk.services.costexplorer.model.Granularity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AwsCostServiceImpl implements AwsCostService {

    @Qualifier("adminCostExplorerClient")
    private final CostExplorerClient ce;

    @Override
    public List<AwsDailyCost> getDailyTotalCost(LocalDate start, LocalDate endExclusive) {
        // 1) 기본 유효성
        if (start == null || endExclusive == null || !endExclusive.isAfter(start)) {
            throw new IllegalArgumentException("end(미포함)는 start보다 커야 합니다.");
        }

        // 2) 페이징 처리 (nextPageToken) — 기간이 길어질 때 대비
        String nextToken = null;
        List<AwsDailyCost> out = new java.util.ArrayList<>();

        do {
            GetCostAndUsageRequest req = GetCostAndUsageRequest.builder()
                    .timePeriod(DateInterval.builder()
                            .start(start.toString())
                            .end(endExclusive.toString())   // 미포함
                            .build())
                    .granularity(Granularity.DAILY)
                    .metrics("UnblendedCost")
                    .nextPageToken(nextToken)
                    .build();

            GetCostAndUsageResponse res = ce.getCostAndUsage(req);

            // 3) metric 누락 방어 (데이터 없을 때 NPE 방지)
            out.addAll(res.resultsByTime().stream().map(r -> {
                var m = r.total().get("UnblendedCost");
                String amountStr = (m == null || m.amount() == null) ? "0" : m.amount();
                String unit = (m == null || m.unit() == null) ? "USD" : m.unit();
                return new AwsDailyCost(
                        LocalDate.parse(r.timePeriod().start()),
                        new BigDecimal(amountStr),
                        unit
                );
            }).toList());

            nextToken = res.nextPageToken(); // 다음 페이지 유무
        } while (nextToken != null && !nextToken.isBlank());

        return out;
    }
}
