package com.cygnus.iptn.interest.service.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InterestResponse {

    private Long id;
    private String name;
    private String iconUrl;
    private Integer sortOrder;
    private boolean active;
    private List<InterestTagResponse> tags;
}
