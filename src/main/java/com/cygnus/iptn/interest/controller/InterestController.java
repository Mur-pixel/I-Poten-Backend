package com.cygnus.iptn.interest.controller;

import com.cygnus.iptn.interest.controller.response_form.InterestResponseForm;
import com.cygnus.iptn.interest.controller.response_form.MyInterestsResponseForm;
import com.cygnus.iptn.interest.controller.response_form.UpdateMyInterestsRequestForm;
import com.cygnus.iptn.interest.mapper.InterestMapper;
import com.cygnus.iptn.interest.service.InterestService;
import com.cygnus.iptn.interest.service.request.UpdateMyInterestsRequest;
import com.cygnus.iptn.interest.service.response.MyInterestsResponse;
import com.cygnus.iptn.redis_cache.RedisCacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class InterestController {

    private final InterestService interestService;
    private final InterestMapper interestMapper;
    private final RedisCacheService redisCacheService;

    @GetMapping("/interests")
    public List<InterestResponseForm> getInterest() {
        return interestMapper.toResponseFormList(interestService.getInterests());
    }

    @GetMapping("/me/interests")
    public MyInterestsResponseForm getMyInterests(
            @CookieValue(name = "userToken", required = false)String userToken
    ) {
        Long accountId = resolveAccountId(userToken);
        MyInterestsResponse response = interestService.getMyInterests(accountId);
        return interestMapper.toResponseForm(response);
    }

    @PutMapping("/me/interests")
    public MyInterestsResponseForm updateMyInterests(
            @CookieValue(name = "userToken", required = false) String userToken,
            @Valid @RequestBody UpdateMyInterestsRequestForm requestForm
    ) {
        Long accountId = resolveAccountId(userToken);
        UpdateMyInterestsRequest request = interestMapper.toRequest(requestForm);
        MyInterestsResponse response = interestService.updateMyInterests(accountId, request);
        return interestMapper.toResponseForm(response);
    }

    public Long resolveAccountId(String userToken) {
        if (userToken == null || userToken.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }

        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        if (accountId == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }

        return accountId;
    }
}
