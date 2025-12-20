package com.cygnus.ipoten.batch.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuizImportRow {

    /** 상단 메타 */
    private String quizKey;           // quiz_key
    private String quizTitle;         // quiz_title

    private String quizSetKey;        // quiz_set_key
    private String quizSetTitle;      // quiz_set_title
    private String quizSetType;       // quiz_set_type (CHOICE/OX/INITIALS 세트 타입 등 - 지금은 import에서 직접 사용 X)

    /** 개별 문항 메타 */
    private String questionKey;       // question_key
    private String questionType;      // question_type (CHOICE/OX/INITIALS)
    private String difficulty;        // difficulty (EASY/MEDIUM/HARD...)
    private String questionText;      // question_text
    private String explanation;       // explanation

    /** 용어 매핑(있으면 Term/Category 찾고, 없으면 null 허용) */
    private Long termId;              // term_id
    private String termTitle;         // term_title (지금은 참고용, 검증/로그에 활용 가능)
    private Long termCategoryId;      // term_category_id
    private String termTopicTagKeys;  // 토픽 태그 키들("react|hooks" / "react, hooks")

    /** 객관식 보기 + 정답 여부 플래그 */
    private String  choice1Text;      // choice_1_text
    private Boolean choice1IsAnswer;  // choice_1_is_answer
    private String  choice2Text;      // choice_2_text
    private Boolean choice2IsAnswer;  // choice_2_is_answer
    private String  choice3Text;      // choice_3_text
    private Boolean choice3IsAnswer;  // choice_3_is_answer
    private String  choice4Text;      // choice_4_text
    private Boolean choice4IsAnswer;  // choice_4_is_answer

    /** 주관식/초성/설명용 텍스트 정답 */
    private String textAnswer;        // text_answer

    /** 기타 메모(지금은 DB에 안 넣고 무시/로그용) */
    private String memo;              // memo
}
