package com.cygnus.ipoten.credit.controller;

import com.cygnus.ipoten.credit.controller.request_form.CreditAccountRequestForm;
import com.cygnus.ipoten.credit.controller.request_form.CreditPayRequestForm;
import com.cygnus.ipoten.credit.controller.response_form.CreditAccountResponseForm;
import com.cygnus.ipoten.credit.controller.response_form.CreditPayResponseForm;
import com.cygnus.ipoten.credit.service.CreditWalletService;
import com.cygnus.ipoten.credit.service.response.CreditAccountResponse;
import com.cygnus.ipoten.credit.service.response.CreditPayResponse;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/credit")
@RequiredArgsConstructor
public class CreditController {

    private final CreditWalletService creditWalletService;
    private final RedisCacheService redisCacheService;

    @GetMapping("/account")
    public ResponseEntity<CreditAccountResponseForm> getCreditByAccount(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        log.info("getCreditByAccount called with userToken: {}", userToken);
        try {
            Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
            if (accountId == null) {
                log.warn("accountId is null for token: {}", userToken);
                return ResponseEntity.status(401).build();
            }
            CreditAccountResponse creditByAccountId = creditWalletService.getCreditByAccountId(accountId);
            return ResponseEntity.ok(creditByAccountId.toCreditAccountResponseForm());
        } catch (Exception e) {
            log.error("Error in getCreditByAccount: ", e);
            throw e;
        }
    }

    @PostMapping("/pay")
    public ResponseEntity<?> pay(
            @RequestBody CreditPayRequestForm creditPayRequestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        log.info("pay called with price: {}, userToken: {}", creditPayRequestForm.getPrice(), userToken);
        try {
            Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
            if (accountId == null) {
                log.warn("accountId is null for token: {}", userToken);
                return ResponseEntity.status(401).body("로그인이 필요합니다.");
            }
            creditPayRequestForm.addAccountId(accountId);
            CreditPayResponse creditPayByAccountId = creditWalletService.getCreditPayByAccountId(creditPayRequestForm);
            CreditPayResponseForm creditPayResponseForm = creditPayByAccountId.toCreditPayResponseForm(creditPayByAccountId);
            return ResponseEntity.ok(creditPayResponseForm);
        } catch (RuntimeException e) {
            if (e.getMessage().equals("크레딧이 부족합니다.")) {
                log.warn("Insufficient credit for accountId: {}", redisCacheService.getValueByKey(userToken, Long.class));
                return ResponseEntity.status(402).body("크레딧이 부족합니다.");
            }
            log.error("Error in pay: ", e);
            return ResponseEntity.status(500).body("결제 처리 중 서버 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("Error in pay: ", e);
            return ResponseEntity.status(500).body("결제 처리 중 알 수 없는 오류가 발생했습니다.");
        }
    }
}
