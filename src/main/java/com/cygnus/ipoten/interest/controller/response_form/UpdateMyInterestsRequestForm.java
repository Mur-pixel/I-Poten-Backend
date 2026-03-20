package com.cygnus.ipoten.interest.controller.response_form;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class UpdateMyInterestsRequestForm {

    @NotNull
    private List<Long> interestsIds;

    @NotNull
    private List<Long> interestTagIds;
}
