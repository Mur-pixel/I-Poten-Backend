package com.cygnus.ipoten.credit.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.credit.entity.CreditWallet;
import com.cygnus.ipoten.credit.repository.CreditWalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreditWalletServiceImplTest {

    @InjectMocks
    private CreditWalletServiceImpl creditWalletService;

    @Mock
    private CreditWalletRepository creditWalletRepository;

    @Mock
    private com.cygnus.ipoten.account.service.AccountService accountService;

    @Test
    @DisplayName("회원가입 크레딧 증정 - 정상 흐름: 지갑에 10 크레딧 적립 및 저장")
    void signedUpCredit_success() {
        // given
        Long accountId = 1L;
        Account account = new Account(accountId);

        CreditWallet wallet = new CreditWallet();
        ReflectionTestUtils.setField(wallet, "account", account);
        ReflectionTestUtils.setField(wallet, "balance", 0L);

        given(accountService.findById(accountId)).willReturn(Optional.of(account));
        given(creditWalletRepository.findByAccount(account)).willReturn(Optional.of(wallet));

        // when
        creditWalletService.signedUpCredit(accountId);

        // then
        assertEquals(10L, wallet.getBalance());
        verify(creditWalletRepository).save(wallet);
    }

    @Test
    @DisplayName("회원가입 크레딧 증정 - 회원을 찾을 수 없음 예외")
    void signedUpCredit_accountNotFound() {
        // given
        Long accountId = 99L;
        given(accountService.findById(accountId)).willReturn(Optional.empty());

        // when
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> creditWalletService.signedUpCredit(accountId));

        // then
        assertEquals("회원가입 이벤트 크레딧 증정 중 회원을 찾을 수 없습니다", ex.getMessage());
    }

    @Test
    @DisplayName("회원가입 크레딧 증정 - 지갑을 찾을 수 없음 예외")
    void signedUpCredit_walletNotFound() {
        // given
        Long accountId = 1L;
        Account account = new Account(accountId);
        given(accountService.findById(accountId)).willReturn(Optional.of(account));
        given(creditWalletRepository.findByAccount(account)).willReturn(Optional.empty());

        // when
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> creditWalletService.signedUpCredit(accountId));

        // then
        assertEquals("회원가입 이벤트 크레딧 증정 중 회원의 크레딧 정보를 찾을 수 없습니다", ex.getMessage());
    }
}
