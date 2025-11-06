// src/main/java/com/wowraid/jobspoon/administer/controller/AdministartorManagementController.java
package com.cygnus.ipoten.administer.controller;

import com.cygnus.ipoten.administer.controller.dto.AdministratorUserInfoRequest;
import com.cygnus.ipoten.administer.service.AdministratorManagementService;
import com.cygnus.ipoten.administer.service.AdministratorOpenAiCostService;
import com.cygnus.ipoten.administer.service.AdministratorService;
import com.cygnus.ipoten.administer.service.dto.AdministratorUserListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/administrator/management")
public class AdministartorManagementController {

    private final AdministratorService administratorService;
    private final AdministratorManagementService administratorManagementService;
    private final AdministratorOpenAiCostService administratorOpenAiCostService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @PostMapping("/userinfo")
    public ResponseEntity<AdministratorUserListResponse> getUserInfo(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody AdministratorUserInfoRequest request
    ) {
        boolean valid = administratorService.isAdminByUserToken(userToken);
        if (!valid) {
            log.info("valid is not matched");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AdministratorUserListResponse payload = administratorManagementService.getUserInfo(request);
        return ResponseEntity.ok(payload);
    }

    @GetMapping(value = "/openaicost", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getOpenAiDailyCost(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
            , @CookieValue(name = "userToken", required = false) String userToken
    ) {

         if (!administratorService.isAdminByUserToken(userToken)) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                     .contentType(MediaType.APPLICATION_JSON)
                     .body("{\"error\":\"unauthorized\"}");
         }

        // 입력 검증
        if (startDate == null || endDate == null) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"start/end must be provided as YYYY-MM-DD\"}");
        }
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"start must be on/before end\"}");
        }
        try {
            // KST 기준 start(포함) / end(포함으로 받았으니 +1일 해서 exclusive로 변환)
            Instant startInclusiveUtc = startDate.atStartOfDay(KST).toInstant();
            Instant endExclusiveUtc   = endDate.plusDays(1).atStartOfDay(KST).toInstant();

            String json = administratorOpenAiCostService.getDailyCosts(startInclusiveUtc, endExclusiveUtc);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json);

        } catch (IllegalArgumentException iae) {
            // 입력/검증 오류 등 클라이언트 측 문제
            log.warn("bad request: {}", iae.toString());
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"" + escape(iae.getMessage()) + "\"}");
        } catch (Exception ex) {
            // 외부 호출(네트워크/키/429/5xx) 등 업스트림 문제
            log.error("upstream error: {}", ex.toString());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"upstream error: " + escape(ex.getMessage()) + "\"}");
        }
    }

    // JSON 안전 출력용 이스케이프
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}