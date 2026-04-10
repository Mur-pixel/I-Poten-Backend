package com.cygnus.iptn.credit.service;

import com.cygnus.iptn.credit.controller.request_form.CreditPayRequestForm;
import com.cygnus.iptn.credit.controller.response_form.CreditAccountResponseForm;
import com.cygnus.iptn.credit.service.response.CreditAccountResponse;
import com.cygnus.iptn.credit.service.response.CreditPayResponse;

public interface CreditWalletService {

    void signedUpCredit(Long AccountId);
    CreditAccountResponse getCreditByAccountId(Long AccountId);
    CreditPayResponse getCreditPayByAccountId(CreditPayRequestForm creditPayRequestForm);

}
