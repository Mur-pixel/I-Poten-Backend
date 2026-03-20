package com.cygnus.ipoten.interest.service.request;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UpdateMyInterestsRequest {

    private List<Long> interestIds;
    private List<Long> interestTagIds;
}
