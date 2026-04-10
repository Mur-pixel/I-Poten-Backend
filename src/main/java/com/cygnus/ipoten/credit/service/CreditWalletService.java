package com.cygnus.ipoten.credit.service;

import com.cygnus.ipoten.credit.controller.request_form.CreditPayRequestForm;
import com.cygnus.ipoten.credit.controller.response_form.CreditAccountResponseForm;
import com.cygnus.ipoten.credit.service.response.CreditAccountResponse;
import com.cygnus.ipoten.credit.service.response.CreditPayResponse;

public interface CreditWalletService {

    void signedUpCredit(Long AccountId);
    CreditAccountResponse getCreditByAccountId(Long AccountId);
    CreditPayResponse getCreditPayByAccountId(CreditPayRequestForm creditPayRequestForm);

}
