package com.cygnus.ipoten.wordbook.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term.repository.TermRepository;
import com.cygnus.ipoten.wordbook.entity.WordbookFolder;
import com.cygnus.ipoten.wordbook.entity.WordbookTerm;
import com.cygnus.ipoten.wordbook.repository.WordbookFolderRepository;
import com.cygnus.ipoten.wordbook.repository.WordbookTermRepository;
import com.cygnus.ipoten.wordbook.service.response.MoveFavoritesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordbookTermServiceImpl implements WordbookTermService {

    private static final String STAR_NORMALIZED = "즐겨찾기";

    private final TermRepository termRepository;
    private final WordbookFolderRepository folderRepository;
    private final WordbookTermRepository termRepo;

    // 즐겨찾기 폴더 보장(없으면 생성)
    private WordbookFolder ensureStarFolder(Long accountId) {
        return folderRepository.findByAccountIdAndNormalizedFolderName(accountId, STAR_NORMALIZED)
                .orElseGet(() -> folderRepository.save(
                        new WordbookFolder(new Account(accountId), "즐겨찾기", 0, STAR_NORMALIZED)
                ));
    }

    // 즐겨찾기 폴더 optional 조회(없으면 빈)
    private Optional<WordbookFolder> findStarFolder(Long accountId) {
        return folderRepository.findByAccountIdAndNormalizedFolderName(accountId, STAR_NORMALIZED);
    }

    @Transactional
    @Override
    public void removeFromStarFolder(Long accountId, Long termId) {
        Optional<WordbookFolder> starOpt = findStarFolder(accountId);
        if (starOpt.isEmpty()) {
            log.debug("[star:remove] star folder not found, noop. accountId={}, termId={}", accountId, termId);
            return;
        }
        int n = termRepo.deleteByAccount_IdAndFolder_IdAndTerm_Id(accountId, starOpt.get().getId(), termId);
        log.info("[star:remove] accountId={}, termId={} -> deleted={}", accountId, termId, n);
    }

    @Transactional
    @Override
    public MoveFavoritesResponse moveFromStarFolder(Long accountId, Long targetFolderId, List<Long> termIds) {
        if (termIds == null || termIds.isEmpty()) {
            return MoveFavoritesResponse.empty(targetFolderId);
        }
        // 타겟 폴더 검증 & 소유 확인
        var target = folderRepository.findById(targetFolderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "폴더를 찾을 수 없습니다."));
        if (!Objects.equals(target.getAccount().getId(), accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        // 스타 폴더(Optional) – 없으면 이동할 원본이 없다고 보고 추가만 수행
        var starOpt = findStarFolder(accountId);

        // 정규화 입력
        List<Long> distinctTermIds = termIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (distinctTermIds.isEmpty()) return MoveFavoritesResponse.empty(targetFolderId);

        Integer base = termRepo.findMaxSortOrderByAccountAndFolder(accountId, target.getId());
        int cursor = (base == null ? 0 : base);

        int moved = 0;
        List<MoveFavoritesResponse.Skipped> skipped = new ArrayList<>();

        for (Long termId : distinctTermIds) {
            // 타겟 폴더 중복 방지
            boolean existsInTarget = termRepo.existsByAccount_IdAndFolder_IdAndTerm_Id(accountId, target.getId(), termId);
            if (existsInTarget) {
                skipped.add(new MoveFavoritesResponse.Skipped(
                        termId, MoveFavoritesResponse.Skipped.Reason.DUPLICATE_IN_TARGET));
                continue;
            }
            // 용어 레퍼런스
            Term termRef = termRepository.findById(termId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "용어를 찾을 수 없습니다. termId=" + termId));

            termRepo.save(WordbookTerm.of(target, termRef, ++cursor));
            moved++;

            // 스타 폴더에서 제거(있을 때만)
            starOpt.ifPresent(star ->
                    termRepo.deleteByAccount_IdAndFolder_IdAndTerm_Id(accountId, star.getId(), termId)
            );
        }

        log.info("[star:move] accountId={}, targetFolderId={}, req={}, moved={}, skipped={}",
                accountId, targetFolderId, distinctTermIds.size(), moved, skipped.size());

        return new MoveFavoritesResponse(targetFolderId, moved, skipped);
    }
}
