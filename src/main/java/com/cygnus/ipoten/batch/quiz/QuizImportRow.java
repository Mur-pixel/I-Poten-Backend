package com.cygnus.ipoten.batch.quiz;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizImportRow {

    /** 문항 키(템플릿/원본 파일 기준) */
    private String questionTempKey;   // question_temp_key (필수)

    /** 용어 표시/매핑 */
    private String termTitle;         // term_title (표시용)
    private Long termId;              // term_id (선택)

    /** 카테고리 표시/매핑 */
    private String termCategory;      // term_category (표시용)
    private Long termCategoryId;      // term_category_id (선택)

    /** 문항 타입/난이도/본문 */
    private String questionType;      // question_type (필수) (CHOICE/OX/INITIALS/TEXT ...)
    private String difficulty;        // difficulty (필수) (EASY/MEDIUM/HARD ...)
    private String questionText;      // question_text (필수)
    private String explanation;       // explanation (선택)

    /** TEXT/INITIALS 등에서 사용하는 정답 텍스트 */
    private String textAnswer;        // text_answer (TEXT일 때 필수)

    /** 객관식 보기 + 정답 여부 플래그 */
    private String  choice1Text;      // choice_1_text
    private Boolean choice1IsAnswer;  // choice_1_is_answer
    private String  choice2Text;      // choice_2_text
    private Boolean choice2IsAnswer;  // choice_2_is_answer
    private String  choice3Text;      // choice_3_text
    private Boolean choice3IsAnswer;  // choice_3_is_answer
    private String  choice4Text;      // choice_4_text
    private Boolean choice4IsAnswer;  // choice_4_is_answer

    /** 라벨(최대 4개) */
    private String label1Key;         // label_1_key (선택)
    private String label2Key;         // label_2_key (선택)
    private String label3Key;         // label_3_key (선택)
    private String label4Key;         // label_4_key (선택)
}