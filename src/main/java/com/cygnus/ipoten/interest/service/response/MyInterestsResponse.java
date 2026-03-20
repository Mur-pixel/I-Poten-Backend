package com.cygnus.ipoten.interest.service.response;

import lombok.Getter;

import java.util.List;

@Getter
public class MyInterestsResponse {

    private final List<Long> interestIds;
    private final List<Long> interestTagIds;

    public MyInterestsResponse(List<Long> interestIds, List<Long> interestTagIds) {
        this.interestIds = interestIds;
        this.interestTagIds = interestTagIds;
    }
}