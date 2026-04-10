package com.cygnus.iptn.interviewee_profile.entity;


import jakarta.persistence.*;
import lombok.Getter;

import java.util.List;

@Entity
@Getter
public class IntervieweeProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String company;       // ex) "당근마켓"
    private String major;         // ex) "전공자"
    private String career;        // ex) "3년 이하"
    private boolean projectExp;    // ex) "있음"
    private String job;           // ex) "Backend"




    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "profile_tech_stack", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "tech_stack")
    private List<TechStack> techStack;

    public IntervieweeProfile(String company, String major, String career, boolean projectExp, String job, List<TechStack> techStack) {

        this.company = company;
        this.major = major;
        this.career = career;
        this.projectExp = projectExp;
        this.job = job;
        this.techStack = techStack;
    }

    public IntervieweeProfile() {
    }
}
