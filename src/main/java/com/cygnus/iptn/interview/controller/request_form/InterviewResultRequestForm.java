package com.cygnus.iptn.interview.controller.request_form;


import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class InterviewResultRequestForm {

    private String userToken;
    private InterviewResultData result;
    private String status;  // "FAILED" (실패 시에만)
    private String error;   // 에러 메시지 (실패 시에만)

    @Getter
    @NoArgsConstructor
    public static class InterviewResultData {
        private Long interview_id;
        private String qna;
        private List<QaScore> qa_scores;
        private String overall_comment;  // 전체 면접 총평
        private Boolean success;
        private EvaluationResult evaluation_result;
    }

    @Getter
    @NoArgsConstructor
    public static class QaScore {
        private String question;
        private String answer;
        private String intent;
        private String feedback;
        private String correction;
    }

    @Getter
    @NoArgsConstructor
    public static class EvaluationResult {
        private Integer communication;
        private Integer productivity;
        private Integer documentation_skills;
        private Integer flexibility;
        private Integer problem_solving;
        private Integer technical_skills;
    }
}
