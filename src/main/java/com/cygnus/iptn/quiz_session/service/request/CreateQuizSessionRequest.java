package com.cygnus.iptn.quiz_session.service.request;

import com.cygnus.iptn.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.iptn.quiz_question.entity.enums.QuestionType;
import com.cygnus.iptn.quiz_session.entity.enums.SeedMode;
import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class CreateQuizSessionRequest {
    private Long accountId;
    private List<QuestionType> questionTypes;
    private Integer count;
    private Integer mcqEach;
    private Integer oxEach;
    private Integer initialsEach;
    private SeedMode seedMode;
    private Long fixedSeed;
    private DifficultyLevel difficulty;
    private final Integer avoidRecentDays;
    private Long folderId;
}
