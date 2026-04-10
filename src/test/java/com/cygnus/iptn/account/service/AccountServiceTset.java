package com.cygnus.iptn.account.service;


import com.cygnus.iptn.account.controller.request_form.RegisterRequestForm;
import com.cygnus.iptn.account.entity.*;
import com.cygnus.iptn.account.exception.UserNotFoundException;
import com.cygnus.iptn.account.repository.AccountLoginTypeRepository;
import com.cygnus.iptn.account.repository.AccountRepository;
import com.cygnus.iptn.account.repository.AccountRoleTypeRepository;
import com.cygnus.iptn.account.service.register_request.RegisterAccountRequest;
import com.cygnus.iptn.authentication.service.AuthenticationService;
import com.cygnus.iptn.redis_cache.RedisCacheServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTset {


    @InjectMocks
    private AccountServiceImpl accountService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountRoleTypeRepository accountRoleTypeRepository;

    @Mock
    private AccountLoginTypeRepository accountLoginTypeRepository;

    @Mock
    private RedisCacheServiceImpl redisCacheService;

    @Mock
    private AuthenticationService authenticationService;


    @Nested
    class createAccount {
        String email = "test_email";
        String nickname = "test_nickname";
        LoginType loginType = LoginType.KAKAO;
        String tmepToken = "tmepToken";

        RegisterRequestForm requestForm = new RegisterRequestForm(
                email,
                nickname,
                loginType
        );

        RegisterAccountRequest request = requestForm.toRegisterAccountRequest();



        @Test
        void RoleType_USER가_DB에_세팅_되어있지_않을_경우_IllegalStateException_예외가_발생합니다(){
            // given
            given(accountRoleTypeRepository.findByRoleType(RoleType.USER)).willReturn(Optional.empty());

            // when
            // then

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                accountService.createAccount(request);
            });

        }

        @Test
        void RoleType_USER가_DB에_세팅_되어있는_경우_예외가_발생하지_않습니다(){
            // given
            given(accountRoleTypeRepository.findByRoleType(RoleType.USER)).willReturn(Optional.of(new AccountRoleType()));
            LoginType loginType = request.getLoginType();
            given(accountLoginTypeRepository.findByLoginType(loginType)).willReturn(Optional.of(new AccountLoginType()));
            // when
            // then

            Assertions.assertDoesNotThrow(() -> {
                accountService.createAccount(request);
            });

        }

        @Test
        void LoginType이_DB에_세팅_되어있지_않을_경우_IllegalStateException_예외가_발생합니다() {

            // given
            given(accountRoleTypeRepository.findByRoleType(RoleType.USER)).willReturn(Optional.of(new AccountRoleType()));
            LoginType loginType = request.getLoginType();
            given(accountLoginTypeRepository.findByLoginType(loginType)).willReturn(Optional.empty());

            // when
            // then
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                accountService.createAccount(request);
            });
        }

        @Test
        void LoginType이_DB에_세팅_되어있는_경우_예외가_발생하지_않습니다(){
            // given
            given(accountRoleTypeRepository.findByRoleType(RoleType.USER)).willReturn(Optional.of(new AccountRoleType()));
            LoginType loginType = request.getLoginType();
            given(accountLoginTypeRepository.findByLoginType(loginType)).willReturn(Optional.of(new AccountLoginType()));

            // when
            // then

            Assertions.assertDoesNotThrow(() -> {
                accountService.createAccount(request);
            });

        }

        @Test
        void account_생성_성공(){

            // given

            given(accountLoginTypeRepository.findByLoginType(loginType)).willReturn(Optional.of(new AccountLoginType(loginType)));
            given(accountRoleTypeRepository.findByRoleType(RoleType.USER)).willReturn(Optional.of(new AccountRoleType(RoleType.USER)));


            // when
            Account account = accountService.createAccount(request).get();
            // then
            Assertions.assertNotNull(account);
            Assertions.assertEquals(account.getAccountLoginType().getLoginType(), loginType);
            Assertions.assertEquals(RoleType.USER, account.getAccountRoleType().getRoleType());
        }



    }


//    @Test
//    void 로그아웃_성공(){
//
//        // given
//        String userToken = "userToken";
//        AccountServiceImpl spyService = Mockito.spy(accountService);
//
//        doReturn(true).when(spyService).deleteToken(userToken);
//        // when
//        boolean result = spyService.logout(userToken);
//
//        // then
//        Assertions.assertTrue(result);
//
//
//    }
//
//
//    @Test
//    void 로그아웃_실패(){
//
//        // given
//        String userToken = "userToken";
//        AccountServiceImpl spyService = Mockito.spy(accountService);
//
//        doReturn(false).when(spyService).deleteToken(userToken);
//
//        // when
//        boolean result = spyService.logout(userToken);
//
//        // then
//        Assertions.assertFalse(result);
//
//    }

    @Test
    void 회원_탈퇴_성공(){

        // given
        String userToken = "userToken";
        Long userId = 1L;
        AccountServiceImpl spyService = Mockito.spy(accountService);

        given(accountRepository.findById(userId)).willReturn(Optional.of(new Account(userId)));
        given(redisCacheService.getValueByKey(userToken, Long.class)).willReturn(userId);
        given(authenticationService.deleteToken(userToken)).willReturn(true);

        // when
        // then
        Assertions.assertDoesNotThrow(() -> {
            spyService.withdraw(userToken);
        });

    }

    @Test
    void 회원_탈퇴_실패(){

        // given
        String userToken = "userToken";
        Long userId = 1L;
        AccountServiceImpl spyService = Mockito.spy(accountService);

        given(accountRepository.findById(userId)).willReturn(Optional.empty());
        given(redisCacheService.getValueByKey(userToken, Long.class)).willReturn(userId);
        given(authenticationService.deleteToken(userToken)).willReturn(true);

        // when


        // then
        Assertions.assertThrows(UserNotFoundException.class, () -> {
            spyService.withdraw(userToken);
        });


    }



}
