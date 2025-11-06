package com.cygnus.ipoten.quiz.batch;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuizImportRow {
    private String setTitle;
    private Boolean setRandom;   // null -> false
    private Long categoryId;     // optional
    private Long termId;         // optional

    private String questionType; // CHOICE/OX/INITIALS
    private String questionText;

    private Integer answerIndex; // CHOICE/OX (1-based)
    private String answerText;   // INITIALS(초성) or OX(O/X) 대체 입력

    private String choice1;      // CHOICE only
    private String choice2;
    private String choice3;
    private String choice4;

    private String explanation;  // optional
    private Integer orderIndex;  // optional
}
