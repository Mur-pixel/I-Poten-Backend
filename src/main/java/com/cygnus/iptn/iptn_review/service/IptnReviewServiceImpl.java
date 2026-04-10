package com.cygnus.iptn.iptn_review.service;

import com.cygnus.iptn.account.entity.Account;
import com.cygnus.iptn.account.service.AccountService;
import com.cygnus.iptn.iptn_review.controller.request_form.IptnReviewRequestForm;
import com.cygnus.iptn.iptn_review.entity.IptnReview;
import com.cygnus.iptn.iptn_review.repository.IptnReviewRepository;
import com.cygnus.iptn.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class IptnReviewServiceImpl implements IptnReviewService {

    private final IptnReviewRepository iptnReviewRepository;
    private final AccountService accountService;
    private final RedisCacheService redisCacheService;

    @Override
    public void registerInterviewReview(String userToken, IptnReviewRequestForm iptnReviewRequestForm) {

        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("서비스 리뷰 때 회원을 찾지 못함"));

        IptnReview iptnReview = IptnReview.builder()
                .account(account)
                .rating(iptnReviewRequestForm.getRating())
                .comment(iptnReviewRequestForm.getComment())
                .build();


        iptnReviewRepository.save(iptnReview);


    }
}
