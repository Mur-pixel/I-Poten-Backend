package com.cygnus.ipoten.administer.service;

import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.administer.controller.dto.AdministratorUserInfoRequest;
import com.cygnus.ipoten.administer.controller.dto.AdministratorUserInfoResponse;
import com.cygnus.ipoten.administer.service.dto.AccountProfileRow;
import com.cygnus.ipoten.administer.service.dto.AdministratorUserListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdministratorManagementServiceImpl implements AdministratorManagementService {
    private final AccountProfileService accountProfileService;

    @Override
    public AdministratorUserListResponse getUserInfo(AdministratorUserInfoRequest request) {
        log.info("AdministratorUserListResponse.getUserInfo is working ");
        final long requestlastId = request.normalizedLastId();// 요청 DTO에서 lastId를 꺼냄 (null이면 0으로 치환됨)
        final int requestPageSize = request.getPageSize();// 요청 DTO에서 pageSize를 꺼냄 (허용값: 30, 50, 70)

        // accountProfileService를 통해 DB에서 lastId 이후 데이터를 (pageSize+1) 만큼 조회
        // size+1을 조회하는 이유: 다음 페이지가 있는지 판별하기 위함
        List<AccountProfileRow> rows;
        try {
            rows = accountProfileService.getProfilesAfterId(requestlastId, requestPageSize + 1);
        } catch (Exception e) {
            throw e;
        }
        boolean hasNext = rows.size() > requestPageSize;
        // hasNext가 true라면 실제 응답은 요청한 개수(pageSize)까지만 잘라서 반환
        if (hasNext) {
            rows = rows.subList(0, requestPageSize);
        }
        // 조회된 AccountProfileRow 리스트를 AdministratorUserInfoResponse DTO로 변환
        // DTO의 getEmail()에서 이메일 마스킹 처리됨
        List<AdministratorUserInfoResponse> items = rows.stream()
                .map(accountProfileRow -> new AdministratorUserInfoResponse(
                        accountProfileRow.getAccountId(),   // 계정 ID
                        accountProfileRow.getNickname(),    // 닉네임
                        accountProfileRow.getEmail()        // 이메일 (마스킹 적용 예정)
                ))
                .toList();
        // nextCursor 계산: 결과가 비어 있으면 기존 lastId 유지, 있으면 마지막 항목의 accountId 사용
        Long nextCursor = items.isEmpty() ? requestlastId : items.get(items.size() - 1).getAccountId();
        // 최종 응답 객체 생성 (목록 items + pageSize + hasNext 여부 + nextCursor)
        return new AdministratorUserListResponse(items, requestPageSize, hasNext, nextCursor);
    }
}
