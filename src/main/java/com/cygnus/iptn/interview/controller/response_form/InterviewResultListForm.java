package com.cygnus.iptn.interview.controller.response_form;


import com.cygnus.iptn.interview.service.response.InterviewResultListResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class InterviewResultListForm {

    private List<InterviewResultListResponse> interviewResultList;

    public InterviewResultListForm(List<InterviewResultListResponse> interviewResultList) {
        this.interviewResultList = interviewResultList;
    }
}
