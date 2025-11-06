package com.cygnus.ipoten.account.service;


import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.controller.request.RegisterAccountProfileRequest;
import com.cygnus.ipoten.accountProfile.repository.AccountProfileRepository;
import com.cygnus.ipoten.accountProfile.service.AccountProfileServiceImp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
public class AccountProfileServiceTest {

    @InjectMocks
    private AccountProfileServiceImp accountProfileService;

    @Mock
    private AccountProfileRepository accountProfileRepository;


    @Nested
    class createAccountProfile {

        String nickname = "nickname";
        String email = "email";

        RegisterAccountProfileRequest request = new RegisterAccountProfileRequest(nickname, email);

        Account account = new Account();

        AccountProfile accountProfile = new AccountProfile(account, nickname, email);



        @Test
        void 닉네임의_값이_없을_경우_예외가_발생합니다() {

            // given
            String nickname = "";
            String email = "email";

            RegisterAccountProfileRequest request = new RegisterAccountProfileRequest(nickname, email);
            // when
            // then
            Assertions.assertThrows(IllegalArgumentException.class, () ->
                    accountProfileService.createAccountProfile(account, request)
            );


        }

        @Test
        void email의_값이_없을_경우_예외가_발생합니다() {

            // given
            String nickname = "nickname";
            String email = "";

            RegisterAccountProfileRequest request = new RegisterAccountProfileRequest(nickname, email);

            // when
            // then
            Assertions.assertThrows(IllegalArgumentException.class, () ->
                    accountProfileService.createAccountProfile(account, request));

        }



        @Test
        void accountProfile을_생성합니다() {

            // given
            given(accountProfileRepository.save(any(AccountProfile.class))).willReturn(accountProfile);

            // when
            AccountProfile accountProfile1 = accountProfileService.createAccountProfile(account, request).get();

            // then
            Assertions.assertNotNull(accountProfile1);
            Assertions.assertEquals(accountProfile.getNickname(), accountProfile1.getNickname());
            Assertions.assertEquals(accountProfile.getEmail(), accountProfile1.getEmail());

        }






    }





}
