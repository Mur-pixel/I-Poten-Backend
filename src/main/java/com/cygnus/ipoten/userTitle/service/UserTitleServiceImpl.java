package com.cygnus.ipoten.userTitle.service;

import com.cygnus.ipoten.userTitle.controller.response.UserTitleResponse;
import com.cygnus.ipoten.userTitle.entity.TitleCode;
import com.cygnus.ipoten.userTitle.entity.UserTitle;
import com.cygnus.ipoten.userTitle.repository.UserTitleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserTitleServiceImpl implements UserTitleService {

    private final UserTitleRepository titleRepository;

    /** 회원가입 시 기본 칭호 생성 **/
    @Override
    @Transactional
    public void initTitle(Long accountId) {
        if (titleRepository.existsByAccountId(accountId)) {
            log.info("✅ 기본 칭호 생성 스킵 - 이미 존재하는 accountId={}", accountId);
            return;
        }

        UserTitle baseTitle = UserTitle.builder()
                .accountId(accountId)
                .titleCode(TitleCode.BEGINNER)
                .acquiredAt(LocalDateTime.now())
                .isEquipped(true)
                .build();

        titleRepository.save(baseTitle);

        log.info("✅ 기본 칭호 생성 완료 - accountId={}, title={}", accountId, baseTitle.getTitleCode());
    }

    /** 칭호 장착 **/
    @Override
    @Transactional
    public UserTitleResponse equipTitle(Long accountId, Long titleId){
        // 1. 모든 칭호 장착 해제
        List<UserTitle> titles = titleRepository.findAllByAccountId(accountId);
        titles.forEach(title -> title.setEquipped(false));

        // 2. 새 칭호 장착
        UserTitle title = titleRepository.findByIdAndAccountId(titleId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Title not owned by this account"));
        title.setEquipped(true);

        return UserTitleResponse.fromEntity(title);
    }

    /** 칭호 장착 해제 **/
    @Override
    @Transactional
    public void unequipTitle(Long accountId){
        List<UserTitle> titles = titleRepository.findAllByAccountId(accountId);
        titles.forEach(title -> title.setEquipped(false));
    }

    /** 칭호 목록 조회 (전체 이력) **/
    @Override
    @Transactional(readOnly = true)
    public List<UserTitleResponse> getMyTitles(Long accountId){
        return titleRepository.findAllByAccountId(accountId).stream()
                .map(UserTitleResponse::fromEntity)
                .toList();
    }
}
