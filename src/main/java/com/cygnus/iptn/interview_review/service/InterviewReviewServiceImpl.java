package com.cygnus.iptn.interview_review.service;

import com.cygnus.iptn.account.entity.Account;
import com.cygnus.iptn.account.service.AccountService;
import com.cygnus.iptn.interview_review.controller.request_form.InterviewReviewRequestForm;
import com.cygnus.iptn.interview_review.entity.InterviewReview;
import com.cygnus.iptn.interview_review.repository.InterviewReviewRepository;
import com.cygnus.iptn.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InterviewReviewServiceImpl implements InterviewReviewService {

    private final InterviewReviewRepository interviewReviewRepository;
    private final RedisCacheService redisCacheService;
    private final AccountService accountService;

    @Override
    public void registerInterviewReview(String userToken,InterviewReviewRequestForm interviewReviewRequestForm) {

        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);

        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰 등록중 회원 정보를 찾을 수 없습니다"));


        InterviewReview interviewReview = InterviewReview.builder()
                .account(account)
                .rating(interviewReviewRequestForm.getRating())
                .comment(interviewReviewRequestForm.getComment())
                .build();

        interviewReviewRepository.save(interviewReview);

    }

}
