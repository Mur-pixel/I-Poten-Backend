package com.cygnus.ipoten.interest.service.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InterestTagResponse {

    private Long id;
    private String name;
    private Integer sortOrder;
    private boolean active;
}
