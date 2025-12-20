package com.cygnus.ipoten.quiz_session_scope.controller.request_form;

import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.cygnus.ipoten.quiz_set.service.request.CreateQuizSetByWordbookRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 단어장 폴더 기반으로 바로 '세션'을 만들기 위한 폼 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateQuizSessionFromWordbookRequestForm {

    @JsonProperty("wordbookId")   private Long wordbookId;
    @JsonProperty("count")      private int count;
    @JsonProperty("questionType") private String questionType;
    @JsonProperty("difficulty") private DifficultyLevel difficulty;
    @JsonProperty("seedMode")   private String seedMode;
    @JsonProperty("fixedSeed")  private Long fixedSeed;
    @JsonProperty("title")      private String title;

    public CreateQuizSetByWordbookRequest toFolderBasedRequest(Long accountId) {
        CreateQuizSetByWordbookRequest.QuestionType qt = parseQuestionType(questionType);
        DifficultyLevel diff =  (difficulty == null ? DifficultyLevel.MEDIUM : difficulty);

        return new CreateQuizSetByWordbookRequest(
                accountId,
                wordbookId,
                count,
                qt,
                diff,
                (title == null || title.isBlank()) ? null : title.trim()
        );
    }

    /** 문자열 questionType -> 서비스 DTO의 enum 매핑. 유효하지 않으면 MIX 기본값 */
    private CreateQuizSetByWordbookRequest.QuestionType parseQuestionType(String raw) {
        if (raw == null) return CreateQuizSetByWordbookRequest.QuestionType.MIX;
        String key = raw.trim().toUpperCase();
        try {
            return CreateQuizSetByWordbookRequest.QuestionType.valueOf(key);
        } catch (IllegalArgumentException e) {
            return CreateQuizSetByWordbookRequest.QuestionType.MIX;
        }
    }
}
