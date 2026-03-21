package com.cygnus.ipoten.accountProfile.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.entity.LoginType;
import com.cygnus.ipoten.accountProfile.controller.request.RegisterAccountProfileRequest;
import com.cygnus.ipoten.accountProfile.controller.response.EmailResponse;
import com.cygnus.ipoten.accountProfile.controller.response.NicknameResponse;
import com.cygnus.ipoten.accountProfile.controller.response.ProfileResponse;
import com.cygnus.ipoten.accountProfile.controller.response.UpdateNicknameResponse;
import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.repository.AccountProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountProfileServiceImpl implements AccountProfileService {


    private final AccountProfileRepository accountProfileRepository;
    private final MyPageSummaryService myPageSummaryService;

    private static final List<String> BANNED_WORDS = List.of(
            "admin", "운영자", "관리자"
    );

    @Override
    public Optional<AccountProfile> createAccountProfile(Account account, RegisterAccountProfileRequest request) {
        String email = requireText(request.getEmail(), "AccountProfile 생성 중 이메일 값이 존재하지 않습니다");
        String nickname = requireText(request.getNickname(), "AccountProfile 생성 중 닉네임 값이 존재하지 않습니다");

        AccountProfile accountProfile = new AccountProfile(account, nickname, email);
        accountProfileRepository.save(accountProfile);

        return Optional.of(accountProfile);
    }

    @Override
    @Transactional
    public Optional<AccountProfile> loadProfileByEmailAndLoginType(String email, LoginType loginType) {
        return accountProfileRepository.findWithAccountByEmailAndLoginType(email, loginType);
    }

    @Override
    public Optional<AccountProfile> loadProfileByEmail(String email) {
        return accountProfileRepository.findWithAccountByEmail(email);
    }

    @Override
    public List<AccountProfileRow> getProfilesAfterId(long lastId, int limit) {
        return accountProfileRepository.findNextProfilesAfterId(lastId, limit);
    }

    private String requireText(String text, String msg) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(msg);
        }
        return text;
    }

    @Override
    public Optional<AccountProfile> loadProfileByEmail(String email) {
        return accountProfileRepository.findWithAccountByEmail(email);
    }

    @Override
    public Optional<UpdateNicknameResponse> updateNickname(Long accountId, String newNickname){
        if(newNickname == null || newNickname.trim().isEmpty()) {
   
            throw new IllegalArgumentException("닉네임은 비워둘 수 없습니다.");
        }

        String trimmed = newNickname.trim();

        if (trimmed.length() < 2 || trimmed.length() > 8) {
            throw new IllegalArgumentException("닉네임은 2자 이상 8자 이하만 가능합니다.");
        }

        if (!trimmed.matches("^[가-힣a-zA-Z0-9]+$")) {
            throw new IllegalArgumentException("닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.");
        }

        for (String banned : BANNED_WORDS) {
            if (trimmed.toLowerCase().contains(banned.toLowerCase())) {
                throw new IllegalArgumentException("사용할 수 없는 단어가 포함되어 있습니다.");
            }
        }


        if (accountProfileRepository.existsByNickname(trimmed)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }


        AccountProfile ap = accountProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("AccountProfile not found"));

        accountProfileRepository.save(ap);

        return Optional.of(new UpdateNicknameResponse(trimmed));
    }

    @Override
    public Optional<AccountProfile> findByAccountId(Long accountId) {
        return accountProfileRepository.findByAccountId(accountId);
    }

    @Override
    public Optional<NicknameResponse> getNicknameByAccountId(Long accountId) {
        AccountProfile accountProfile = accountProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("닉네임을 찾는 중 회원을 찾을 수 없습니다"));

        return Optional.of(new NicknameResponse(accountProfile.getNickname()));
    }

    @Override
    public Optional<EmailResponse> getEmailByAccountId(Long accountId) {
        AccountProfile accountProfile = accountProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("이메일을 찾는 중 회원을 찾을 수 없습니다"));

        return Optional.of(new EmailResponse(accountProfile.getEmail()));
    }

    @Override
    public Optional<ProfileResponse> getProfileByAccountId(Long accountId) {
        AccountProfile accountProfile = accountProfileRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("프로필을 찾는 중 회원을 찾을 수 없습니다"));

        MyPageSummaryService.RepresentativeProfile representative =
                myPageSummaryService.resolveRepresentativeProfile(accountId).orElse(null);

        return Optional.of(new ProfileResponse(
                accountProfile.getEmail(),
                accountProfile.getNickname(),
                myPageSummaryService.resolveLastActivityAt(accountId),
                representative != null ? representative.label() : null,
                representative != null ? representative.job() : null,
                representative != null ? representative.career() : null
        ));
    }
}
