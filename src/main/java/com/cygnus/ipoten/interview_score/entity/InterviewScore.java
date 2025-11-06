package com.cygnus.ipoten.interview_score.entity;

import com.cygnus.ipoten.interview.entity.Interview;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class InterviewScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Interview interview;

    @Column(name = "communication")
    private int communication;

    @Column(name = "productivity")
    private int productivity;

    @Column(name = "documentation_skills")
    private int documentationSkills;

    @Column(name = "flexibility")
    private int flexibility;

    @Column(name = "problem_solving")
    private int problemSolving;

    @Column(name = "technical_skills")
    private int technicalSkills;


    public InterviewScore() {
    }

    public InterviewScore(Interview interview, int communication, int productivity, int documentationSkills, int flexibility, int problemSolving, int technicalSkills) {
        this.interview = interview;
        this.communication = communication;
        this.productivity = productivity;
        this.documentationSkills = documentationSkills;
        this.flexibility = flexibility;
        this.problemSolving = problemSolving;
        this.technicalSkills = technicalSkills;
    }
}
