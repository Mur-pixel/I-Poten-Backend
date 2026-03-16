package com.cygnus.ipoten.credit.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.service.AccountService;
import com.cygnus.ipoten.credit.controller.request_form.CreditPayRequestForm;
import com.cygnus.ipoten.credit.controller.response_form.CreditAccountResponseForm;
import com.cygnus.ipoten.credit.entity.CreditTransaction;
import com.cygnus.ipoten.credit.entity.CreditTransactionType;
import com.cygnus.ipoten.credit.entity.CreditWallet;
import com.cygnus.ipoten.credit.repository.CreditTransactionRepository;
import com.cygnus.ipoten.credit.repository.CreditWalletRepository;
import com.cygnus.ipoten.credit.service.response.CreditAccountResponse;
import com.cygnus.ipoten.credit.service.response.CreditPayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreditWalletServiceImpl implements CreditWalletService {

    private final CreditWalletRepository creditWalletRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final AccountService accountService;

    @Override
    public void signedUpCredit(Long AccountId) {
        Account foundAccount = accountService.findById(AccountId)
                .orElseThrow(() -> new IllegalArgumentException("회원가입 이벤트 크레딧 증정 중 회원을 찾을 수 없습니다"));


        CreditWallet creditWallet = new CreditWallet(foundAccount, 10L);
        CreditTransaction creditTransaction = new CreditTransaction(
                creditWallet, CreditTransactionType.BONUS, 10L, 10L, "신규 가입자 이벤트");
        creditWalletRepository.save(creditWallet);
        creditTransactionRepository.save(creditTransaction);

        log.info("토큰 주입 성공");
        log.info("크레딧 : {}", creditWallet.getBalance());


    }

    @Override
    public CreditAccountResponse getCreditByAccountId(Long AccountId) {
        CreditWallet creditWallet = creditWalletRepository.findByAccountId(AccountId)
                .orElseGet(() -> {
                    log.info("지갑이 없는 사용자(ID: {})를 위해 기본 지갑을 생성합니다.", AccountId);
                    Account foundAccount = accountService.findById(AccountId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + AccountId));
                    
                    CreditWallet newWallet = new CreditWallet(foundAccount, 10L);
                    CreditTransaction creditTransaction = new CreditTransaction(
                            newWallet, CreditTransactionType.BONUS, 10L, 10L, "시스템 자동 생성 보너스");
                    
                    creditWalletRepository.save(newWallet);
                    creditTransactionRepository.save(creditTransaction);
                    return newWallet;
                });
        return new CreditAccountResponse(creditWallet.getBalance());
    }

    @Override
    public CreditPayResponse getCreditPayByAccountId(CreditPayRequestForm creditPayRequestForm) {


        int result = creditWalletRepository.useCredit(
                creditPayRequestForm.getAccountId(),
                creditPayRequestForm.getPrice()
        );

        if (result == 0) {
            throw new RuntimeException("크레딧이 부족합니다.");
        }

        CreditWallet wallet = creditWalletRepository
                .findByAccountId(creditPayRequestForm.getAccountId())
                .orElseThrow(() -> new RuntimeException("지갑 없음"));

        return new CreditPayResponse(wallet.getBalance());

    }
}
