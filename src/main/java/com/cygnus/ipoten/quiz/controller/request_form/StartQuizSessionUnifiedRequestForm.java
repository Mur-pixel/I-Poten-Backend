package com.cygnus.ipoten.quiz.controller.request_form;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartQuizSessionUnifiedRequestForm {

    @NotBlank(message = "source는 필수입니다")
    // DAILY까지 허용
    @Pattern(regexp="(?i)folder|category|set|daily", message = "source는 folder|category|set|daily 이어야 합니다")
    private String source;      // "folder" | "category" | "set" | "daily"

    @JsonAlias({"userFolderId","userWordbookFolderId","folder_id"})
    private Long folderId;      // source=folder 일 때 필수

    @JsonAlias({"category_id"})
    private Long categoryId;    // source=category 일 때 필수

    /** set/daily 모드에서는 무시됨 */
    private Integer count;      // 1~100 (folder/category일 때만)

    @JsonAlias({"questionType"})
    // DAILY_* 도 허용 (fromParam이 정규화하더라도 검증 단계에서 막히지 않게)
    @Pattern(
            regexp="(?i)mix|choice|ox|initials|daily_choice|daily_ox|daily_initials",
            message = "type은 mix|choice|ox|initials|daily_choice|daily_ox|daily_initials 이어야 합니다"
    )
    private String type;        // folder/category/daily 에서 사용 (set은 무시)

    @JsonAlias({"difficulty"})
    @Pattern(regexp="(?i)mix|easy|medium|hard", message = "level은 mix|easy|medium|hard 이어야 합니다")
    private String level;       // folder/category일 때만 필수

    @JsonAlias({"seed_mode"})
    private String seedMode;    // "AUTO" | "DAILY" | "FIXED" (정책에 맞게)

    private Long fixedSeed;     // FIXED일 때 시드값

    @JsonAlias({"quizSetId","set_id"})
    private Long setId;         // source=set 일 때 필수

    // 데일리용 날짜(없으면 오늘로 처리)
    @JsonAlias({"dt","quizDate"})
    private String date;        // yyyy-MM-dd or null/blank

    // 직무/역할 (없으면 GENERAL)
    @JsonAlias({"jobRole"})
    private String role;        // e.g. "GENERAL"

    /* ---------- NORMALIZE (검증 전에 호출됨) ---------- */
    public void setSource(String source) {
        if (source == null) { this.source = null; return; }
        String s = source.trim();
        if ("folder".equalsIgnoreCase(s) || "category".equalsIgnoreCase(s)
                || "set".equalsIgnoreCase(s) || "daily".equalsIgnoreCase(s)) {
            this.source = s.toLowerCase();
        } else {
            this.source = s; // 패턴에서 걸리게 그대로 둠
        }
    }

    @AssertTrue(message = "source=folder 일 때 folderId가 필요합니다")
    public boolean isFolderIdValid() {
        return !"folder".equalsIgnoreCase(source) || folderId != null;
    }

    @AssertTrue(message = "source=category 일 때 categoryId가 필요합니다")
    public boolean isCategoryIdValid() {
        return !"category".equalsIgnoreCase(source) || categoryId != null;
    }

    @AssertTrue(message = "source=set 일 때 setId가 필요합니다")
    public boolean isSetIdValid() {
        return !"set".equalsIgnoreCase(source) || setId != null;
    }

    @AssertTrue(message = "count는 source in [folder,category] 일 때 1~100 사이여야 합니다")
    public boolean isCountValid() {
        if (!"folder".equalsIgnoreCase(source) && !"category".equalsIgnoreCase(source)) return true;
        return count != null && count >= 1 && count <= 100;
    }

    @AssertTrue(message = "type은 source in [folder,category] 일 때 필수입니다")
    public boolean isTypeValid() {
        if (!"folder".equalsIgnoreCase(source) && !"category".equalsIgnoreCase(source)) return true;
        return type != null && !type.isBlank();
    }

    @AssertTrue(message = "level은 source in [folder,category] 일 때 필수입니다")
    public boolean isLevelValid() {
        if (!"folder".equalsIgnoreCase(source) && !"category".equalsIgnoreCase(source)) return true;
        return level != null && !level.isBlank();
    }

    public StartQuizSessionByCategoryRequestForm toCategoryForm() {
        StartQuizSessionByCategoryRequestForm f = new StartQuizSessionByCategoryRequestForm();
        f.setCategoryId(this.categoryId);
        f.setCount(this.count != null ? this.count : 0);
        f.setQuestionType(this.type);
        f.setDifficulty(this.level);
        f.setSeedMode(this.seedMode);
        f.setFixedSeed(this.fixedSeed);
        return f;
    }

    public CreateQuizSessionFromFolderRequestForm toFolderForm() {
        CreateQuizSessionFromFolderRequestForm f = new CreateQuizSessionFromFolderRequestForm();
        f.setFolderId(this.folderId);
        f.setCount(this.count != null ? this.count : 0);
        f.setQuestionType(this.type);
        f.setDifficulty(this.level);
        f.setSeedMode(this.seedMode);
        f.setFixedSeed(this.fixedSeed);
        return f;
    }
}