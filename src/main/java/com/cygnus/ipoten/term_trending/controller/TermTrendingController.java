package com.cygnus.ipoten.term_trending.controller;

import com.cygnus.ipoten.common.annotation.PublicEndpoint;
import com.cygnus.ipoten.term_trending.controller.request_form.TrendingTermRequestForm;
import com.cygnus.ipoten.term_trending.controller.response_form.TrendingTermResponseForm;
import com.cygnus.ipoten.term_trending.service.TermTrendingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PublicEndpoint
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "TermTrending", description = "인기 검색(트렌딩) 용어 조회 API")
public class TermTrendingController {

    private final TermTrendingService termTrendingService;

    @GetMapping("terms/trending")
    public ResponseEntity<TrendingTermResponseForm> trending(
            @Valid @ModelAttribute TrendingTermRequestForm requestForm
    ) {
        try {
            var response = termTrendingService.getTrending(requestForm.toRequest());
            return ResponseEntity.ok(TrendingTermResponseForm.from(response));
        } catch (Exception e) {
            log.error("[TermTrendingController] trending 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
