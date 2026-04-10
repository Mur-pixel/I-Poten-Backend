package com.cygnus.iptn.credit.service.response;

import com.cygnus.iptn.credit.controller.request_form.CreditPayRequestForm;
import com.cygnus.iptn.credit.controller.response_form.CreditPayResponseForm;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreditPayResponse {

    private Long currentCredit;


    public CreditPayResponseForm toCreditPayResponseForm(CreditPayResponse creditPayResponse) {
        return new CreditPayResponseForm(
                this.currentCredit
        );
    }
}
