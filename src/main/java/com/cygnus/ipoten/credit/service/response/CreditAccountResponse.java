package com.cygnus.ipoten.credit.service.response;

import com.cygnus.ipoten.credit.controller.response_form.CreditAccountResponseForm;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreditAccountResponse {

    private final Long credit;

    public CreditAccountResponseForm toCreditAccountResponseForm() {
        return new CreditAccountResponseForm(credit);
    }


}
