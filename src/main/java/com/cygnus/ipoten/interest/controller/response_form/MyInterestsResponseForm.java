package com.cygnus.ipoten.interest.controller.response_form;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MyInterestsResponseForm {

    private List<Long> interestIds;
    private List<Long> interestTagIds;
}