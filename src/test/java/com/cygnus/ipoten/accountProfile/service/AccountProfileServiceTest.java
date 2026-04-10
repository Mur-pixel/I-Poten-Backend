package com.cygnus.ipoten.accountProfile.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.accountProfile.controller.response.EmailResponse;
import com.cygnus.ipoten.accountProfile.controller.response.NicknameResponse;
import com.cygnus.ipoten.accountProfile.controller.response.ProfileResponse;
import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.repository.AccountProfileRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class AccountProfileServiceTest{

    @InjectMocks
    private AccountProfileServiceImp accountProfileService;

    @Mock
    private AccountProfileRepository accountProfileRepository;



    @Test
    @DisplayName("사용자의_식별_아이디를_통해_닉네임을_찾습니다")
    void 사용자의_식별_아이디를_통해_닉네임을_찾습니다(){

        // given
        Long testAccountId = 1L;
        Account account = new Account();
        String testNickname = "testNickname";
        String testEmail = "testEmail";
        AccountProfile accountProfile = new AccountProfile(account, testNickname, testEmail);

        given(accountProfileRepository.findByAccountId(testAccountId)).willReturn(Optional.of(accountProfile));
        // when
        NicknameResponse nicknameResponse = accountProfileService.getNicknameByAccountId(testAccountId)
                .orElseThrow(() -> new IllegalArgumentException("닉네임 가져오는 기능 테스트 증 오류발생"));
        // then

        Assertions.assertEquals(testNickname, nicknameResponse.getNickname());

    }

    @Test
    @DisplayName("사용자의_식별_아이디를_통해_이메일을_찾습니다")
    void 사용자의_식별_아이디를_통해_이메일을_찾습니다(){

        // given
        Long testAccountId = 1L;
        Account account = new Account();
        String testNickname = "testEmail";
        String testEmail = "testEmail";
        AccountProfile accountProfile = new AccountProfile(account, testNickname, testEmail);
        given(accountProfileRepository.findByAccountId(testAccountId)).willReturn(Optional.of(accountProfile));

        // when
        EmailResponse emailResponse = accountProfileService.getEmailByAccountId(testAccountId)
                .orElseThrow(() -> new IllegalArgumentException("닉네임 가져오는 기능 테스트 증 오류발생"));

        // then

        Assertions.assertEquals(testNickname, emailResponse.getEmail());

    }


    @Test
    @DisplayName("사용자의_식별_아이디를_통해_회원정보를_찾습니다")
    void 사용자의_식별_아이디를_통해_회원정보를_찾습니다(){

        // given
        Long testAccountId = 1L;
        Account account = new Account();
        String testNickname = "testEmail";
        String testEmail = "testEmail";
        AccountProfile accountProfile = new AccountProfile(account, testNickname, testEmail);
        given(accountProfileRepository.findByAccountId(testAccountId)).willReturn(Optional.of(accountProfile));

        // when
        ProfileResponse profileResponse = accountProfileService.getProfileByAccountId(testAccountId)
                .orElseThrow(() -> new IllegalArgumentException("닉네임 가져오는 기능 테스트 증 오류발생"));

        // then

        Assertions.assertEquals(testEmail, profileResponse.getEmail());
        Assertions.assertEquals(testNickname, profileResponse.getNickname());

    }



}
