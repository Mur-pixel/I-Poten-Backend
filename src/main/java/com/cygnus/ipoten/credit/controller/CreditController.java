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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        CreditAccountResponse creditByAccountId = creditWalletService.getCreditByAccountId(accountId);
        return ResponseEntity.ok(creditByAccountId.toCreditAccountResponseForm());

    }

    @PostMapping("/pay")
    public ResponseEntity<CreditPayResponseForm> pay(
            @RequestBody CreditPayRequestForm creditPayRequestForm,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        creditPayRequestForm.addAccountId(redisCacheService.getValueByKey(userToken, Long.class));
        CreditPayResponse creditPayByAccountId = creditWalletService.getCreditPayByAccountId(creditPayRequestForm);
        CreditPayResponseForm creditPayResponseForm = creditPayByAccountId.toCreditPayResponseForm(creditPayByAccountId);
        return ResponseEntity.ok(creditPayResponseForm);


    }
}
