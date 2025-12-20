package com.cygnus.ipoten.quiz_session_scope.controller.request_form;

import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.job.enums.JobRole;
import com.cygnus.ipoten.quiz_set.service.request.CreateQuizSetByCategoryRequest;
import com.cygnus.ipoten.quiz_set.service.request.CreateQuizSetByJobRoleRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateQuizSetByJobRoleRequestForm {

    @NotBlank(message = "jobRole은 필수입니다")
    private String roleRaw;     // JobRole.from에 들어갈 raw 문자열 (ex: "FRONTEND")

    @Min(1) @Max(100)
    private int count;          // 문항 수

    // "mix|choice|ox|initials"
    private String questionType;

    // mix / easy / medium / hard
    private DifficultyLevel difficulty;

    public CreateQuizSetByJobRoleRequest toServiceRequest(JobRole jobRole) {

        CreateQuizSetByCategoryRequest.QuestionType qt = null;
        if (questionType != null && !questionType.isBlank()) {
            String t = questionType.trim().toUpperCase();
            switch (t) {
                case "CHOICE"   -> qt = CreateQuizSetByCategoryRequest.QuestionType.CHOICE;
                case "OX"       -> qt = CreateQuizSetByCategoryRequest.QuestionType.OX;
                case "INITIALS" -> qt = CreateQuizSetByCategoryRequest.QuestionType.INITIALS;
                case "MIX"      -> qt = CreateQuizSetByCategoryRequest.QuestionType.MIX;
                default         -> qt = CreateQuizSetByCategoryRequest.QuestionType.CHOICE;
            }
        }

        return CreateQuizSetByJobRoleRequest.builder()
                .jobRole(jobRole)
                .count(count)
                .questionType(qt)
                .difficulty(difficulty != null ? difficulty : DifficultyLevel.MEDIUM)
                .build();
    }
}
