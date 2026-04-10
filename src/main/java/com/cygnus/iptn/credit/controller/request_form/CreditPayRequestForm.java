package com.cygnus.iptn.credit.controller.request_form;

import lombok.Getter;

@Getter
public class CreditPayRequestForm {

    private Long price;
    private Long accountId;


    public void addAccountId(Long accountId) {
        this.accountId = accountId;
    }


}
