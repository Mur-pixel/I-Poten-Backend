package com.cygnus.ipoten.quiz_session_scope.controller.request_form;

import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.quiz_session_scope.value_objects.*;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartQuizSessionUnifiedRequestForm {

    @NotBlank(message = "source는 필수입니다")
    @Pattern(
            regexp = "(?i)wordbook|term_category|set|wrong_note",
            message = "source는 wordbook|term_category|set|wrong_note 중 하나여야 합니다"
    )
    private String source;

    private List<Long> questionIds;

    @Size(max = 50, message = "title은 50자 이하여야 합니다.")
    @JsonAlias({"title", "customTitle"})
    private String customTitle;

    @JsonAlias({"wordbookId","wordbook_id"})
    private Long wordbookId;      // source=wordbook 일 때 필수

    @JsonAlias({"termCategoryId","term_category_id"})
    private Long termCategoryId;      // source=term_category 일 때 필수

    /** set 일부 모드에서는 무시될 수 있음 */
    @JsonAlias({"count", "totalQuestions", "total_questions"})
    private Integer count;        // 1~100 (wordbook / term_category일 때 유효성 검증)

    @JsonAlias({"questionType"})
    @Pattern(
            regexp="(?i)mix|choice|ox|initials",
            message = "type은 mix|choice|ox|initials이어야 합니다"
    )
    private String type;          // wordbook / term_category 에서 사용 (set은 무시될 수 있음)

    @JsonAlias({"difficulty"})
    private DifficultyLevel level;         // wordbook / term_category 일 때 필수

    @JsonAlias({"seed_mode"})
    private String seedMode;

    private Long fixedSeed;

    @JsonAlias({"quizSetId","quiz_set_id","setId","set_id"})
    private Long quizSetId;           // source=set 일 때 필수

    @JsonAlias({"labelKeys","label_keys"})
    private List<String> labelKeys;   // ex) ["react"], ["vue"]

    /* ---------- NORMALIZE (검증 전에 호출됨) ---------- */
    public void setSource(String source) {
        if (source == null) {
            this.source = null;
            return;
        }
        String s = source.trim();

        // 구버전 호환 정규화
        // folder → wordbook
        // category/job → term_category
        if ("folder".equalsIgnoreCase(s)) {
            s = "wordbook";
        } else if ("category".equalsIgnoreCase(s) || "job".equalsIgnoreCase(s)) {
            s = "term_category";
        } else if ("wrongnote".equalsIgnoreCase(s) || "wrong-note".equalsIgnoreCase(s) || "wrong".equalsIgnoreCase(s)) {
            s = "wrong_note";
        }

        if ("wordbook".equalsIgnoreCase(s)
                || "term_category".equalsIgnoreCase(s)
                || "set".equalsIgnoreCase(s)
                || "wrong_note".equalsIgnoreCase(s)) {
            this.source = s.toLowerCase();
        } else {
            this.source = s;
        }
    }

    private boolean isSourceOneOf(String... candidates) {
        if (source == null) return false;
        for (String c : candidates) {
            if (c.equalsIgnoreCase(source)) return true;
        }
        return false;
    }

    @AssertTrue(message = "source=wordbook 일 때 wordbookId가 필요합니다")
    public boolean isWordbookIdValid() {
        return !isSourceOneOf("wordbook") || wordbookId != null;
    }

    @AssertTrue(message = "source=term_category 일 때 categoryId가 필요합니다")
    public boolean isCategoryIdValid() {
        return !isSourceOneOf("term_category") || termCategoryId != null;
    }

    @AssertTrue(message = "source=set 일 때 quizSetId가 필요합니다")
    public boolean isSetScopeValid() {
        return !isSourceOneOf("set") || quizSetId != null;
    }

    @AssertTrue(message = "count는 source in [wordbook, term_category] 일 때 1~100 사이여야 합니다")
    public boolean isCountValid() {
        if (!isSourceOneOf("wordbook", "term_category")) return true;
        return count != null && count >= 1 && count <= 100;
    }

    @AssertTrue(message = "type은 source in [wordbook, term_category] 일 때 필수입니다")
    public boolean isTypeValid() {
        if (!isSourceOneOf("wordbook", "term_category")) return true;
        return type != null && !type.isBlank();
    }

    @AssertTrue(message = "level은 source in [wordbook, term_category] 일 때 필수입니다")
    public boolean isLevelValid() {
        if (!isSourceOneOf("wordbook", "term_category")) return true;
        return level != null;
    }

    @AssertTrue(message = "source=wrong_note 일 때 questionIds가 필요합니다")
    public boolean isWrongNoteIdsValid() {
        if (!isSourceOneOf("wrong_note")) return true;
        return questionIds != null && !questionIds.isEmpty();
    }

    /* ---------- 기존 폼으로 위임 변환 ---------- */

    public StartQuizSessionByCategoryRequestForm toCategoryForm() {
        StartQuizSessionByCategoryRequestForm f = new StartQuizSessionByCategoryRequestForm();
        f.setCategoryId(this.termCategoryId);
        f.setCount(this.count != null ? this.count : 0);
        f.setQuestionType(this.type);
        f.setDifficulty(this.level != null ? this.level.name() : null);
        return f;
    }

    public CreateQuizSessionFromWordbookRequestForm toWordbookForm() {
        CreateQuizSessionFromWordbookRequestForm f = new CreateQuizSessionFromWordbookRequestForm();
        f.setWordbookId(this.wordbookId);
        f.setCount(this.count != null ? this.count : 0);
        f.setQuestionType(this.type);
        f.setDifficulty(this.level);
        return f;
    }

    public ScopeCondition toScopeCondition(Long accountId) {
        SeedPolicy seedPolicy = SeedPolicy.fromRaw(seedMode, fixedSeed);
        String src = source == null ? "" : source.trim().toLowerCase();

        return switch (src) {
            case "wordbook" -> ScopeCondition.forWordbook(
                    new WordbookScope(
                            accountId,
                            wordbookId,
                            count,
                            QuestionTypeScope.fromRaw(type),
                            DifficultyScope.from(level)
                    ),
                    seedPolicy
            );

            case "term_category" -> {
                String safeType = (type == null || type.isBlank()) ? "mix" : type;
                DifficultyLevel safeLevel = (level == null) ? DifficultyLevel.MEDIUM : level;
                List<String> safeLabels = (labelKeys == null) ? List.of() : labelKeys;

                yield ScopeCondition.forCategory(
                        new TermCategoryScope(
                                termCategoryId,
                                count,
                                QuestionTypeScope.fromRaw(safeType),
                                DifficultyScope.from(safeLevel),
                                safeLabels
                        ),
                        seedPolicy
                );
            }

            case "set" -> ScopeCondition.forSet(
                    SetScope.ofId(quizSetId, count, type, level),
                    seedPolicy
            );

            case "wrong_note" -> ScopeCondition.forWrongNote(
                    new WrongNoteScope(accountId, questionIds),
                    seedPolicy,
                    customTitle
            );

            default -> throw new IllegalArgumentException("지원하지 않는 source: " + source);
        };
    }
}
