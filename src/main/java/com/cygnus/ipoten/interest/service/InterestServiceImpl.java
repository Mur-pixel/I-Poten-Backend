package com.cygnus.ipoten.interest.service.impl;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.interest.entity.AccountInterest;
import com.cygnus.ipoten.interest.entity.AccountInterestTag;
import com.cygnus.ipoten.interest.entity.Interest;
import com.cygnus.ipoten.interest.entity.InterestTag;
import com.cygnus.ipoten.interest.repository.AccountInterestRepository;
import com.cygnus.ipoten.interest.repository.AccountInterestTagRepository;
import com.cygnus.ipoten.interest.repository.InterestRepository;
import com.cygnus.ipoten.interest.repository.InterestTagRepository;
import com.cygnus.ipoten.interest.service.InterestService;
import com.cygnus.ipoten.interest.service.request.UpdateMyInterestsRequest;
import com.cygnus.ipoten.interest.service.response.InterestResponse;
import com.cygnus.ipoten.interest.service.response.InterestTagResponse;
import com.cygnus.ipoten.interest.service.response.MyInterestsResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestServiceImpl implements InterestService {

    private static final int MAX_INTEREST_COUNT = 5;
    private static final int MAX_INTEREST_TAG_COUNT = 20;

    private final InterestRepository interestRepository;
    private final InterestTagRepository interestTagRepository;
    private final AccountInterestRepository accountInterestRepository;
    private final AccountInterestTagRepository accountInterestTagRepository;

    @Override
    public List<InterestResponse> getInterests() {
        List<Interest> interests = interestRepository.findByActiveTrueOrderBySortOrderAscIdAsc();
        List<Long> interestIds = interests.stream()
                .map(Interest::getId)
                .toList();

        List<InterestTag> tags = interestIds.isEmpty()
                ? List.of()
                : interestTagRepository.findByInterestIdInAndActiveTrueOrderBySortOrderAscIdAsc(interestIds);

        Map<Long, List<InterestTag>> tagsByInterestId = tags.stream()
                .collect(Collectors.groupingBy(tag -> tag.getInterest().getId()));

        return interests.stream()
                .map(interest -> InterestResponse.builder()
                        .id(interest.getId())
                        .name(interest.getName())
                        .iconUrl(interest.getIconUrl())
                        .sortOrder(interest.getSortOrder())
                        .active(interest.isActive())
                        .tags(
                                tagsByInterestId.getOrDefault(interest.getId(), List.of())
                                        .stream()
                                        .map(tag -> InterestTagResponse.builder()
                                                .id(tag.getId())
                                                .name(tag.getName())
                                                .sortOrder(tag.getSortOrder())
                                                .active(tag.isActive())
                                                .build())
                                        .toList()
                        )
                        .build())
                .toList();
    }

    @Override
    public MyInterestsResponse getMyInterests(Long accountId) {
        List<Long> interestIds = accountInterestRepository.findByAccountId(accountId)
                .stream()
                .map(accountInterest -> accountInterest.getInterest().getId())
                .sorted()
                .toList();

        List<Long> interestTagIds = accountInterestTagRepository.findByAccountId(accountId)
                .stream()
                .map(accountInterestTag -> accountInterestTag.getInterestTag().getId())
                .sorted()
                .toList();

        return new MyInterestsResponse(interestIds, interestTagIds);
    }

    @Override
    @Transactional
    public MyInterestsResponse updateMyInterests(Long accountId, UpdateMyInterestsRequest request) {
        validateRequired(request);

        List<Long> normalizedInterestIds = normalizeUnique(
                request.getInterestIds(),
                "중복된 관심 분야 ID가 포함되어 있습니다."
        );
        List<Long> normalizedInterestTagIds = normalizeUnique(
                request.getInterestTagIds(),
                "중복된 관심 태그 ID가 포함되어 있습니다."
        );

        if (normalizedInterestIds.isEmpty()) {
            throw new IllegalArgumentException("관심 분야는 최소 1개 이상 선택해야 합니다.");
        }

        if (normalizedInterestIds.size() > MAX_INTEREST_COUNT) {
            throw new IllegalArgumentException("관심 분야는 최대 " + MAX_INTEREST_COUNT + "개까지 선택할 수 있습니다.");
        }

        if (normalizedInterestTagIds.size() > MAX_INTEREST_TAG_COUNT) {
            throw new IllegalArgumentException("관심 태그는 최대 " + MAX_INTEREST_TAG_COUNT + "개까지 선택할 수 있습니다.");
        }

        List<Interest> interests = interestRepository.findByIdInAndActiveTrue(normalizedInterestIds);
        validateInterestExistence(normalizedInterestIds, interests);

        List<InterestTag> interestTags = normalizedInterestTagIds.isEmpty()
                ? List.of()
                : interestTagRepository.findByIdInAndActiveTrue(normalizedInterestTagIds);

        validateInterestTagExistence(normalizedInterestTagIds, interestTags);
        validateTagBelongsToSelectedInterests(normalizedInterestIds, interestTags);

        accountInterestTagRepository.deleteByAccountId(accountId);
        accountInterestRepository.deleteByAccountId(accountId);

        Account accountReference = new Account(accountId);

        List<AccountInterest> accountInterests = interests.stream()
                .map(interest -> new AccountInterest(accountReference, interest))
                .toList();
        accountInterestRepository.saveAll(accountInterests);

        List<AccountInterestTag> accountInterestTags = interestTags.stream()
                .map(tag -> new AccountInterestTag(accountReference, tag))
                .toList();
        accountInterestTagRepository.saveAll(accountInterestTags);

        return new MyInterestsResponse(normalizedInterestIds, normalizedInterestTagIds);
    }

    private void validateRequired(UpdateMyInterestsRequest request) {
        if (request.getInterestIds() == null) {
            throw new IllegalArgumentException("interestIds는 필수입니다.");
        }
        if (request.getInterestTagIds() == null) {
            throw new IllegalArgumentException("interestTagIds는 필수입니다.");
        }
    }

    private List<Long> normalizeUnique(List<Long> values, String duplicateMessage) {
        long nonNullCount = values.stream()
                .filter(Objects::nonNull)
                .count();

        List<Long> distinctValues = values.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (distinctValues.size() != nonNullCount) {
            throw new IllegalArgumentException(duplicateMessage);
        }

        return distinctValues;
    }

    private void validateInterestExistence(List<Long> requestedIds, List<Interest> foundInterests) {
        Set<Long> foundIds = foundInterests.stream()
                .map(Interest::getId)
                .collect(Collectors.toSet());

        List<Long> invalidIds = requestedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new EntityNotFoundException("존재하지 않거나 비활성화된 관심 분야가 포함되어 있습니다. invalidIds=" + invalidIds);
        }
    }

    private void validateInterestTagExistence(List<Long> requestedIds, List<InterestTag> foundTags) {
        Set<Long> foundIds = foundTags.stream()
                .map(InterestTag::getId)
                .collect(Collectors.toSet());

        List<Long> invalidIds = requestedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new EntityNotFoundException("존재하지 않거나 비활성화된 관심 태그가 포함되어 있습니다. invalidIds=" + invalidIds);
        }
    }

    private void validateTagBelongsToSelectedInterests(List<Long> interestIds, List<InterestTag> interestTags) {
        Set<Long> selectedInterestIdSet = new HashSet<>(interestIds);

        List<Long> invalidTagIds = interestTags.stream()
                .filter(tag -> !selectedInterestIdSet.contains(tag.getInterest().getId()))
                .map(InterestTag::getId)
                .toList();

        if (!invalidTagIds.isEmpty()) {
            throw new IllegalArgumentException("선택하지 않은 관심 분야의 태그가 포함되어 있습니다. invalidTagIds=" + invalidTagIds);
        }
    }
}