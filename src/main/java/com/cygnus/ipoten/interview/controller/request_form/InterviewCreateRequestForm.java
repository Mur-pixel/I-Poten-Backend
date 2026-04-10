package com.cygnus.ipoten.interview.controller.request_form;


import com.cygnus.ipoten.interview.controller.request.InterviewAccountProjectRequest;
import com.cygnus.ipoten.interview.controller.request.InterviewQARequest;
import com.cygnus.ipoten.interview.controller.request.IntervieweeProfileRequest;
import com.cygnus.ipoten.interview.entity.Interview;
import com.cygnus.ipoten.interview.entity.InterviewType;
import com.cygnus.ipoten.interviewee_profile.entity.TechStack;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InterviewCreateRequestForm {

    private InterviewType interviewType;
    private String company;       // ex) "당근마켓"
    private String major;         // ex) "전공자"
    private String career;        // ex) "3년 이하"
    private boolean projectExp;    // ex) "있음"
    private String job;           // ex) "Backend"
    private List<InterviewAccountProjectRequest> interviewAccountProjectRequests;

    // ex) "job-spoon 프로젝트는 ai 면접...."
    private List<TechStack> techStacks;  // ex) "풀스택, 백엔드, ..."
    private String firstQuestion;
    private String firstAnswer;


    public InterviewQARequest toInterviewQARequest(Interview interview) {
        return new InterviewQARequest(interview, firstQuestion,firstAnswer);
    }

    public IntervieweeProfileRequest toIntervieweeProfileRequest() {
        return new IntervieweeProfileRequest(
                company,major,career,projectExp,job,interviewAccountProjectRequests,techStacks
        );
    }




}
