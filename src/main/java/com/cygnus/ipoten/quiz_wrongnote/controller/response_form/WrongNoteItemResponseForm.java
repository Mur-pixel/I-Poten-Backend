package com.cygnus.ipoten.quiz_wrongnote.controller.response_form;

import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_wrongnote.entity.QuizWrongNote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WrongNoteItemResponseForm {

    private Long wrongNoteId;
    private Long quizSessionId;
    private Long questionId;

    private Long termId;
    private String termTitle;
    private String categoryLabel;

    private String questionType;
    private String difficulty;

    private String questionText;
    private String explanation;

    private String myAnswer;
    private String correctAnswer;

    private List<ChoiceItem> choices;
    private Instant wrongAt;
    private Long wrongCount;
    private String badgeLabel;

    private boolean resolved;
    private String status;

    public static WrongNoteItemResponseForm from(
            QuizWrongNote wn,
            List<QuizChoice> choices,
            String myAnswer,
            String correctAnswer,
            Long wrongCount,
            boolean resolved,
            boolean includeAnswers
    ) {
        var q = wn.getQuizQuestion();
        var term = q.getTerm();

        Long termId = null;
        String termTitle = null;
        String categoryLabel = null;

        if (term != null) {
            termId = term.getId();
            termTitle = term.getTitle();

            var cat = term.getTermCategory();
            if (cat != null) {
                categoryLabel = cat.getName();
            }
        }

        List<ChoiceItem> choiceItems = (choices == null ? List.of() : choices.stream()
                .map(c -> ChoiceItem.builder()
                        .choiceId(c.getId())
                        .choiceText(c.getChoiceText())
                        .isAnswer(includeAnswers ? c.isAnswer() : null)
                        .build())
                .toList());

        var s = resolved ? "RESOLVED" : "UNRESOLVED";

        return WrongNoteItemResponseForm.builder()
                .wrongNoteId(wn.getId())
                .quizSessionId(wn.getQuizSessionId())
                .questionId(q.getId())
                .questionType(String.valueOf(q.getQuestionType()))
                .difficulty(String.valueOf(q.getDifficulty()))
                .questionText(q.getQuestionText())
                .explanation(q.getExplanation())
                .myAnswer(myAnswer)
                .correctAnswer(includeAnswers ? correctAnswer : null)
                .choices(choiceItems)
                .wrongAt(wn.getSubmittedAt())
                .wrongCount(wrongCount)
                .badgeLabel(buildBadgeLabel(wrongCount))
                .termId(termId)
                .termTitle(termTitle)
                .categoryLabel(categoryLabel)
                .status(s)
                .resolved(resolved)
                .build();
    }

    private static String buildBadgeLabel(Long wrongCount) {
        if (wrongCount == null || wrongCount <= 1L) {
            return null;
        }
        return wrongCount + "회 오답";
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChoiceItem {
        private Long choiceId;
        private String choiceText;
        private Boolean isAnswer;
    }
}
