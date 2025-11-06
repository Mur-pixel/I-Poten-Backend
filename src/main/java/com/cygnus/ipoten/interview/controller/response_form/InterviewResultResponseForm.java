package com.cygnus.ipoten.interview.controller.response_form;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
public class InterviewResultResponseForm {
    private List<Qa> interviewResultList;
    private HexagonScore hexagonScore;
    private String overallComment;

    @Getter
    @NoArgsConstructor
    public static class Qa {
        private String question;
        private String answer;
        private String intent;
        private String feedback;
        private String correction;

        public Qa(String question, String answer, String intent, String feedback, String correction) {
            this.question = question;
            this.answer = answer;
            this.intent = intent;
            this.feedback = feedback;
            this.correction = correction;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class HexagonScore {
        private Integer productivity;
        private Integer communication;
        private Integer technical_skills;
        private Integer documentation_skills;
        private Integer flexibility;
        private Integer problem_solving;

        public HexagonScore(Integer productivity, Integer communication, Integer technical_skills, Integer documentation_skills, Integer flexibility, Integer problem_solving) {
            this.productivity = productivity;
            this.communication = communication;
            this.technical_skills = technical_skills;
            this.documentation_skills = documentation_skills;
            this.flexibility = flexibility;
            this.problem_solving = problem_solving;
        }
    }

    public InterviewResultResponseForm() {
    }

    public InterviewResultResponseForm(List<Qa> interviewResultList, HexagonScore hexagonScore, String overallComment) {
        this.interviewResultList = interviewResultList;
        this.hexagonScore = hexagonScore;
        this.overallComment = overallComment;
    }
}
