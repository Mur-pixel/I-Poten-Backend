package com.cygnus.iptn.credit.event;

import com.cygnus.iptn.credit.service.CreditWalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class SignupCreditListenerTest {

    @Test
    @DisplayName("회원가입 이벤트 수신 시 크레딧 서비스 호출")
    void handle_callsService() {
        // given
        CreditWalletService creditWalletService = Mockito.mock(CreditWalletService.class);
        SignupCreditListener listener = new SignupCreditListener(creditWalletService);
        AccountSignedUpEvent event = new AccountSignedUpEvent(123L);

        // when: private 메서드를 리플렉션으로 호출
        ReflectionTestUtils.invokeMethod(listener, "handle", event);

        // then
        Mockito.verify(creditWalletService).signedUpCredit(123L);
    }
}
