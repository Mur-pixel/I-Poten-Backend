package com.cygnus.ipoten.accountProfile.service;


import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.entity.LoginType;
import com.cygnus.ipoten.accountProfile.controller.response.EmailResponse;
import com.cygnus.ipoten.accountProfile.controller.response.ProfileResponse;
import com.cygnus.ipoten.accountProfile.controller.response.UpdateNicknameResponse;
import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.controller.request.RegisterAccountProfileRequest;
import com.cygnus.ipoten.accountProfile.controller.response.NicknameResponse;
import com.cygnus.ipoten.administer.service.dto.AccountProfileRow;

import java.util.List;
import java.util.Optional;

public interface AccountProfileService {
    Optional<AccountProfile> createAccountProfile(Account account, RegisterAccountProfileRequest request);
    Optional<AccountProfile> loadProfileByEmailAndLoginType(String email, LoginType loginType);
    //2025.09.13 발키리 추가
    Optional<AccountProfile> loadProfileByEmail(String email);
    List<AccountProfileRow> getProfilesAfterId(long lastId, int limit);
    // 닉네임 수정
    Optional<UpdateNicknameResponse> updateNickname(Long accountId, String newNickname);
    Optional<AccountProfile> findByAccountId(Long accountId);

    //닉네임 찾기
    Optional<NicknameResponse> getNicknameByAccountId(Long accountId);

    Optional<EmailResponse> getEmailByAccountId(Long accountId);

    Optional<ProfileResponse> getProfileByAccountId(Long accountId);

}
