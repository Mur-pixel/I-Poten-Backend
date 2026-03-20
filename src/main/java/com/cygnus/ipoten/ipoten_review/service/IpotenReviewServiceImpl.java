package com.cygnus.ipoten.ipoten_review.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.service.AccountService;
import com.cygnus.ipoten.ipoten_review.controller.request_form.IpotenReviewRequestForm;
import com.cygnus.ipoten.ipoten_review.entity.IpotenReview;
import com.cygnus.ipoten.ipoten_review.repository.IpotenReviewRepository;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class IpotenReviewServiceImpl implements IpotenReviewService {

    private final IpotenReviewRepository ipotenReviewRepository;
    private final AccountService accountService;
    private final RedisCacheService redisCacheService;

    @Override
    public void registerInterviewReview(String userToken, IpotenReviewRequestForm ipotenReviewRequestForm) {

        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("서비스 리뷰 때 회원을 찾지 못함"));

        IpotenReview ipotenReview = IpotenReview.builder()
                .account(account)
                .rating(ipotenReviewRequestForm.getRating())
                .comment(ipotenReviewRequestForm.getComment())
                .build();


        ipotenReviewRepository.save(ipotenReview);


    }
}
