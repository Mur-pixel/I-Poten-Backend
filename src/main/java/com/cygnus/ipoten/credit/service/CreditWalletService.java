package com.cygnus.ipoten.credit.service;

import com.cygnus.ipoten.credit.controller.response_form.CreditAccountResponseForm;
import com.cygnus.ipoten.credit.service.response.CreditAccountResponse;

public interface CreditWalletService {

    void signedUpCredit(Long AccountId);
    CreditAccountResponse getCreditByAccountId(Long AccountId);

}
