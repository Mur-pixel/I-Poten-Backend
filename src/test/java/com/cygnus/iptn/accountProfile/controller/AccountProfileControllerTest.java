package com.cygnus.iptn.accountProfile.controller;

import com.cygnus.iptn.accountProfile.controller.response.EmailResponse;
import com.cygnus.iptn.accountProfile.controller.response.NicknameResponse;
import com.cygnus.iptn.accountProfile.controller.response.ProfileResponse;
import com.cygnus.iptn.accountProfile.service.AccountProfileService;
import com.cygnus.iptn.redis_cache.RedisCacheService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class AccountProfileControllerTest {


    @InjectMocks
    private AccountProfileController accountProfileController;
    @Mock
    private AccountProfileService accountProfileService;
    @Mock
    RedisCacheService redisCacheService;


    @Test
    @DisplayName("사용자의_닉네임을_가져옵니다")
    void 사용자의_닉네임을_가져옵니다() {

        // given
        String testToken = "testToken";
        Long testAccountId = 1L;
        String testUserNickname = "testUserNickname";
        NicknameResponse nicknameResponse = new NicknameResponse(testUserNickname);

        given(redisCacheService.getValueByKey(testToken, Long.class)).willReturn(testAccountId);
        given(accountProfileService.getNicknameByAccountId(testAccountId)).willReturn(Optional.of(nicknameResponse));


        // when
        ResponseEntity<NicknameResponse> nickname = accountProfileController.getNickname(testToken);

        // then
        Assertions.assertNotNull(nickname.getBody());
        Assertions.assertEquals(testUserNickname, nickname.getBody().getNickname());


    }


    @Test
    @DisplayName("사용자의_이메일을_가져옵니다")
    void 사용자의_이메일을_가져옵니다() {

        // given
        String testToken = "testToken";
        Long testAccountId = 1L;
        String testUserEmail = "testUserEmail";
        EmailResponse emailResponse = new EmailResponse(testUserEmail);

        given(redisCacheService.getValueByKey(testToken, Long.class)).willReturn(testAccountId);
        given(accountProfileService.getEmailByAccountId(testAccountId)).willReturn(Optional.of(emailResponse));

        // when
        ResponseEntity<EmailResponse> email = accountProfileController.getEmail(testToken);

        // then
        Assertions.assertNotNull(email.getBody());
        Assertions.assertEquals(testUserEmail, email.getBody().getEmail());


    }



    @Test
    @DisplayName("사용자의_회원정보를_가져옵니다")
    void 사용자의_회원정보를_가져옵니다() {

        // given
        String testToken = "testToken";
        Long testAccountId = 1L;
        String testUserNickname = "testUserNickname";
        String testUserEmail = "testUserEmail";

        ProfileResponse profileResponse = new ProfileResponse(testUserEmail, testUserNickname);
        given(redisCacheService.getValueByKey(testToken, Long.class)).willReturn(testAccountId);
        given(accountProfileService.getProfileByAccountId(testAccountId)).willReturn(Optional.of(profileResponse));

        // when
        ResponseEntity<ProfileResponse> Profile = accountProfileController.getProfile(testToken);

        // then
        Assertions.assertNotNull(Profile.getBody());
        Assertions.assertEquals(testUserNickname, Profile.getBody().getNickname());
        Assertions.assertEquals(testUserEmail, Profile.getBody().getEmail());


    }




}
