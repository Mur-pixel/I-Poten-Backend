package com.cygnus.iptn.interest.controller.response_form;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InterestTagResponseForm {

    private Long id;
    private String name;
    private Integer sortOrder;
    private boolean active;
}
