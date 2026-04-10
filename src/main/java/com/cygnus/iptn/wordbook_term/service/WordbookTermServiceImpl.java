package com.cygnus.iptn.wordbook_term.service;

import com.cygnus.iptn.account.entity.Account;
import com.cygnus.iptn.term.entity.Term;
import com.cygnus.iptn.term.repository.TermRepository;
import com.cygnus.iptn.wordbook.entity.Wordbook;
import com.cygnus.iptn.wordbook.service.response.MoveWordbookTermsResponse;
import com.cygnus.iptn.wordbook_term.entity.WordbookTerm;
import com.cygnus.iptn.wordbook.repository.WordbookRepository;
import com.cygnus.iptn.wordbook_term.repository.WordbookTermRepository;
import com.cygnus.iptn.wordbook.service.response.MoveFavoritesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordbookTermServiceImpl implements WordbookTermService {

    private final TermRepository termRepository;
    private final WordbookRepository wordbookRepository;
    private final WordbookTermRepository wordbookTermRepository;

    /** 단어장 폴더에 있는 용어를 다른 단어장으로 이동 */
    @Override
    @Transactional
    @Caching(evict = { @CacheEvict(value = "wordbookTerms", allEntries = true) })
    public MoveWordbookTermsResponse moveTerms(
            Long accountId, Long sourceWordbookId, Long targetWordbookId, List<Long> termIds) {

        if (Objects.equals(sourceWordbookId, targetWordbookId)) {
            log.info("[moveTerms] SAME_WORDBOOK wordbookId={} requestedIds={}",
                    sourceWordbookId, termIds);
            final List<Long> ids = (termIds == null) ? Collections.emptyList() : termIds;
            return MoveWordbookTermsResponse.sameFolder(sourceWordbookId, ids);
        }
        if (termIds == null || termIds.isEmpty()) {
            return new MoveWordbookTermsResponse(sourceWordbookId, targetWordbookId, 0, List.of(), List.of());
        }

        // 폴더 소유 검증
        var source = wordbookRepository.findById(sourceWordbookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "소스 폴더를 찾을 수 없습니다."));
        var target = wordbookRepository.findById(targetWordbookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "대상 폴더를 찾을 수 없습니다."));

        if (!Objects.equals(source.getAccount().getId(), accountId) ||
                !Objects.equals(target.getAccount().getId(), accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        // 입력 정규화
        List<Long> distinctTermIds = termIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 결과 집계 변수들
        int moved = 0;
        List<Long> movedTermIds = new ArrayList<>();
        List<MoveWordbookTermsResponse.Skipped> skipped = new ArrayList<>();

        // 대상 폴더의 현재 최대 sortOrder (account 조건 제거 버전)
        int baseOrder = Optional.ofNullable(
                wordbookTermRepository.findMaxSortOrderByWordbook(targetWordbookId)
        ).orElse(0);
        int cursor = baseOrder;

        for (Long termId : distinctTermIds) {
            var srcOpt = wordbookTermRepository.findByWordbook_IdAndTerm_Id(sourceWordbookId, termId);
            if (srcOpt.isEmpty()) {
                skipped.add(new MoveWordbookTermsResponse.Skipped(
                        termId, MoveWordbookTermsResponse.Skipped.Reason.NOT_IN_SOURCE));
                continue;
            }

            boolean inTarget = wordbookTermRepository
                    .existsByAccount_IdAndWordbook_IdAndTerm_Id(accountId, targetWordbookId, termId);
            if (inTarget) {
                skipped.add(new MoveWordbookTermsResponse.Skipped(
                        termId, MoveWordbookTermsResponse.Skipped.Reason.DUPLICATE_IN_TARGET));
                continue;
            }

            Term termRef = termRepository.getReferenceById(termId);
            var created = WordbookTerm.of(target, termRef, ++cursor); // account는 wordbook에서 자동 세팅
            wordbookTermRepository.save(created);

            wordbookTermRepository.deleteById(srcOpt.get().getId());
            moved++;
            movedTermIds.add(termId);
        }

        int dupe = (int) skipped.stream()
                .filter(s -> s.getReason() == MoveWordbookTermsResponse.Skipped.Reason.DUPLICATE_IN_TARGET)
                .count();
        int notInSrc = (int) skipped.stream()
                .filter(s -> s.getReason() == MoveWordbookTermsResponse.Skipped.Reason.NOT_IN_SOURCE)
                .count();
        int notFound = (int) skipped.stream()
                .filter(s -> s.getReason() == MoveWordbookTermsResponse.Skipped.Reason.TERM_NOT_FOUND)
                .count();

        log.info("[moveTerms] {} -> {} | requested={} moved={} skipped={} (dupe={}, notInSource={}, notFound={}) | movedTermIds={}",
                sourceWordbookId, targetWordbookId,
                distinctTermIds.size(), moved, skipped.size(), dupe, notInSrc, notFound, movedTermIds);

        return new MoveWordbookTermsResponse(sourceWordbookId, targetWordbookId, moved, skipped, movedTermIds);
    }

    /** 단어장 폴더에 있는 용어 삭제 */
    @Override
    @Transactional
    public void removeTermsFromWordbook(Long accountId, Long wordbookId, List<Long> termIds) {

        // 1) 폴더 소유자 확인
        var wordbook = wordbookRepository.findById(wordbookId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "단어장을 찾을 수 없습니다."));

        if (!wordbook.getAccount().getId().equals(accountId)) {
            throw new ResponseStatusException(FORBIDDEN, "해당 단어장에 대한 권한이 없습니다.");
        }

        // 2) term 매핑 삭제
        wordbookTermRepository.deleteByAccountIdAndWordbookIdAndTermIdIn(accountId, wordbookId, termIds);

        log.info("[wordbook:remove-terms] {}개 term 제거 완료", termIds.size());
    }
}
