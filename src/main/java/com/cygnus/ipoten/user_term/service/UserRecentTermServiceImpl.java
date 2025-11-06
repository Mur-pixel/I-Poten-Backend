package com.cygnus.ipoten.user_term.service;

import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term.repository.TermRepository;
import com.cygnus.ipoten.user_term.entity.UserRecentTerm;
import com.cygnus.ipoten.user_term.repository.UserRecentTermRepository;
import com.cygnus.ipoten.user_term.service.request.RecordTermViewRequest;
import com.cygnus.ipoten.user_term.service.response.RecordTermViewResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRecentTermServiceImpl implements UserRecentTermService {

    private final UserRecentTermRepository userRecentTermRepository;
    private final TermRepository termRepository;


    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public RecordTermViewResponse recordTermView(RecordTermViewRequest request) {
        final Long accountId = request.getAccountId();
        final Long termId = request.getTermId();
        final LocalDateTime now = LocalDateTime.now();

        // Account 존재 / 유효성 검사
        if(request.getAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Account-Id 헤더가 필요합니다.");
        }

        // Term 존재 / 유효성 검사
        Term term = termRepository.findById(request.getTermId())
                .orElseThrow(() -> new IllegalArgumentException("용어가 존재하지 않습니다."));

        // 업서트 시도 : 존재하는 경우 touch(), 없는 경우 create()
        UserRecentTerm userRecentTerm = userRecentTermRepository.findByAccountIdAndTerm_Id(accountId, termId)
                .map(existing -> {
                    existing.touch(now);
                    return existing;
                })
                .orElseGet(()->{
//                    Account accountRef = em.getReference(Account.class, accountId);
                    Term termRef = em.getReference(Term.class, termId); // 불필요 SELECT 방지
                    return userRecentTermRepository.save(UserRecentTerm.create(accountId, termRef, now));
                });

        return new RecordTermViewResponse(
                userRecentTerm.getId(),
                userRecentTerm.getTerm().getId(),
                userRecentTerm.getFirstSeenAt(),
                userRecentTerm.getLastSeenAt(),
                userRecentTerm.getViewCount()
        );
    }
}
