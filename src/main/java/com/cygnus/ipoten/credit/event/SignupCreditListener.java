package com.cygnus.ipoten.credit.event;
import com.cygnus.ipoten.credit.service.CreditWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SignupCreditListener {

    private final CreditWalletService creditWalletService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(AccountSignedUpEvent accountSingedUpEvent) {
        creditWalletService.signedUpCredit(accountSingedUpEvent.getAccountId());
    }


}
