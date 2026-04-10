package com.cygnus.iptn.interest.service;

import com.cygnus.iptn.interest.service.request.UpdateMyInterestsRequest;
import com.cygnus.iptn.interest.service.response.InterestResponse;
import com.cygnus.iptn.interest.service.response.MyInterestsResponse;

import java.util.List;

public interface InterestService {

    List<InterestResponse> getInterests();

    MyInterestsResponse getMyInterests(Long accountId);

    MyInterestsResponse updateMyInterests(Long accountId, UpdateMyInterestsRequest request);
}