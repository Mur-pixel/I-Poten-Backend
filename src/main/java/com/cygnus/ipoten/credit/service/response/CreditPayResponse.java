package com.cygnus.ipoten.credit.service.response;

import com.cygnus.ipoten.credit.controller.request_form.CreditPayRequestForm;
import com.cygnus.ipoten.credit.controller.response_form.CreditPayResponseForm;
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
