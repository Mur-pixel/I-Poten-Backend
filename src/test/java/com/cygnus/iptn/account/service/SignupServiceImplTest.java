package com.cygnus.iptn.account.service;

import com.cygnus.iptn.account.controller.request_form.RegisterRequestForm;
import com.cygnus.iptn.account.entity.Account;
import com.cygnus.iptn.account.entity.LoginType;
import com.cygnus.iptn.account.service.register_request.RegisterAccountRequest;
import com.cygnus.iptn.account.service.register_response.RegisterResponse;
import com.cygnus.iptn.accountProfile.controller.request.RegisterAccountProfileRequest;
import com.cygnus.iptn.accountProfile.entity.AccountProfile;
import com.cygnus.iptn.authentication.service.AuthenticationService;
import com.cygnus.iptn.credit.event.AccountSignedUpEvent;
import com.cygnus.iptn.infrastructure.external.email.EmailService;
import com.cygnus.iptn.redis_cache.RedisCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SignupServiceImplTest {

    @InjectMocks
    private SignupServiceImpl signupService;

    @Mock private AccountService accountService;
    @Mock private com.cygnus.iptn.accountProfile.service.AccountProfileService accountProfileService;
    @Mock private RedisCacheService redisCacheService;
    @Mock private AuthenticationService authenticationService;
    @Mock private EmailService emailService;
    @Mock private org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    @Test
    @DisplayName("회원가입 성공 시 이벤트 발행 및 응답 반환")
    void signup_publishesEvent_and_returnsResponse() {
        // given
        String tempToken = "temp-token";
        String accessToken = "access-token";
        RegisterRequestForm form = new RegisterRequestForm("test@example.com", "tester", LoginType.KAKAO);

        Account account = new Account(1L);
        AccountProfile profile = new AccountProfile(account, "tester", "test@example.com");

        given(redisCacheService.getValueByKey(tempToken, String.class)).willReturn(accessToken);
        given(accountService.createAccount(any(RegisterAccountRequest.class))).willReturn(Optional.of(account));
        given(accountProfileService.createAccountProfile(any(Account.class), any(RegisterAccountProfileRequest.class)))
                .willReturn(Optional.of(profile));
        given(authenticationService.deleteToken(tempToken)).willReturn(true);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        // when
        RegisterResponse response = signupService.signup(tempToken, form);

        // then: 응답 값 검증
        assertNotNull(response);
        assertEquals("tester", response.getNickname());
        assertEquals("test@example.com", response.getEmail());
        assertNotNull(response.getUserToken());
        assertFalse(response.getUserToken().isEmpty());

        // then: 부수효과 검증
        verify(redisCacheService).setKeyAndValue(account.getId(), accessToken);
        verify(authenticationService).deleteToken(tempToken);
        verify(emailService).sendSignupWelcomeEmail("test@example.com", "tester");

        // then: 이벤트 발행 검증
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        Object published = eventCaptor.getValue();
        assertTrue(published instanceof AccountSignedUpEvent);
        assertEquals(1L, ((AccountSignedUpEvent) published).getAccountId());
    }

    @Test
    @DisplayName("Account 생성 실패 시 예외")
    void signup_accountCreateFails_throws() {
        // given
        String tempToken = "temp-token";
        RegisterRequestForm form = new RegisterRequestForm("test@example.com", "tester", LoginType.KAKAO);
        given(redisCacheService.getValueByKey(tempToken, String.class)).willReturn("access-token");
        given(accountService.createAccount(any(RegisterAccountRequest.class))).willReturn(Optional.empty());

        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> signupService.signup(tempToken, form));
        assertEquals("Account 생성 실패", ex.getMessage());
    }
}
