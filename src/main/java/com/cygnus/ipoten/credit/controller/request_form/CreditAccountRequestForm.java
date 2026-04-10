package com.cygnus.ipoten.credit.controller.request_form;

import com.cygnus.ipoten.credit.service.request.CreditAccountRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreditAccountRequestForm {

    private final Long accountId;

    public CreditAccountRequest toRequest() {
        return new CreditAccountRequest(accountId);
    }

}
