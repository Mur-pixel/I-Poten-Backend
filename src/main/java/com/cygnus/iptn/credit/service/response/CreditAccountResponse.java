package com.cygnus.iptn.credit.service.response;

import com.cygnus.iptn.credit.controller.response_form.CreditAccountResponseForm;
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
