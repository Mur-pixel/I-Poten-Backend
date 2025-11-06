package com.cygnus.ipoten.quiz.service.request;

import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz.entity.enums.SeedMode;
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
    private String difficulty;
    private final Integer avoidRecentDays;
    private Long folderId;
}
