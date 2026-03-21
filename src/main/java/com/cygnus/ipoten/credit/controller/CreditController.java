package com.cygnus.ipoten.credit.controller;

import com.cygnus.ipoten.common.annotation.LoginUser;
import com.cygnus.ipoten.credit.controller.request_form.CreditPayRequestForm;
import com.cygnus.ipoten.credit.controller.response_form.CreditAccountResponseForm;
import com.cygnus.ipoten.credit.controller.response_form.CreditPayResponseForm;
import com.cygnus.ipoten.credit.service.CreditWalletService;
import com.cygnus.ipoten.credit.service.response.CreditAccountResponse;
import com.cygnus.ipoten.credit.service.response.CreditPayResponse;
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

    @GetMapping("/account")
    public ResponseEntity<CreditAccountResponseForm> getCreditByAccount(@LoginUser Long accountId) {
        try {
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
            @LoginUser Long accountId) {
        try {
            creditPayRequestForm.addAccountId(accountId);
            CreditPayResponse creditPayByAccountId = creditWalletService.getCreditPayByAccountId(creditPayRequestForm);
            CreditPayResponseForm creditPayResponseForm = creditPayByAccountId.toCreditPayResponseForm(creditPayByAccountId);
            return ResponseEntity.ok(creditPayResponseForm);
        } catch (RuntimeException e) {
            if (e.getMessage().equals("크레딧이 부족합니다.")) {
                log.warn("Insufficient credit for accountId: {}", accountId);
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
