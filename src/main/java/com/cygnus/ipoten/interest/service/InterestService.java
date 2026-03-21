package com.cygnus.ipoten.interest.service;

import com.cygnus.ipoten.interest.service.request.UpdateMyInterestsRequest;
import com.cygnus.ipoten.interest.service.response.InterestResponse;
import com.cygnus.ipoten.interest.service.response.MyInterestsResponse;

import java.util.List;

public interface InterestService {

    List<InterestResponse> getInterests();

    MyInterestsResponse getMyInterests(Long accountId);

    MyInterestsResponse updateMyInterests(Long accountId, UpdateMyInterestsRequest request);
}