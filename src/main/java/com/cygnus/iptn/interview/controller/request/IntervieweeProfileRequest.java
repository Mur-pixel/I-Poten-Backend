package com.cygnus.iptn.interview.controller.request;

import com.cygnus.iptn.interviewee_profile.entity.TechStack;
import lombok.Getter;

import java.util.List;

@Getter
public class IntervieweeProfileRequest {

    private String company;       // ex) "당근마켓"
    private String major;         // ex) "전공자"
    private String career;        // ex) "3년 이하"
    private boolean projectExp;    // ex) "있음"
    private String job;           // ex) "Backend"
    private List<InterviewAccountProjectRequest> interviewAccountProjectRequests;
    // ex) "job-spoon 프로젝트는 ai 면접...."
    private List<TechStack> techStacks;  // ex) "풀스택, 백엔드, ..."

    public IntervieweeProfileRequest(String company, String major, String career, boolean projectExp, String job, List<InterviewAccountProjectRequest> interviewAccountProjectRequests, List<TechStack> techStacks) {
        this.company = company;
        this.major = major;
        this.career = career;
        this.projectExp = projectExp;
        this.job = job;
        this.interviewAccountProjectRequests = interviewAccountProjectRequests;
        this.techStacks = techStacks;
    }

    public IntervieweeProfileRequest() {
    }
}
