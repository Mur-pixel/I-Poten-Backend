package com.cygnus.ipoten.interest.controller.response_form;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InterestResponseForm {

    private Long id;
    private String name;
    private String iconUrl;
    private Integer sortOrder;
    private boolean active;
    private List<InterestTagResponseForm> tags;
}
