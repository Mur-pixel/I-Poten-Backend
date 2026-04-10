package com.cygnus.iptn.batch.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizSetImportRow {

    /** 세트 제목 */
    private String setTitle;          // set_title (필수)

    /** 문항 키(템플릿/원본 파일 기준) */
    private String questionTempKey;   // question_temp_key (필수)

    /** 세트 내 순서 */
    private Integer orderNo;          // order_no (선택)

    /** 세트 타입(표시용) */
    private String setType;           // set_type (선택)

    /** 문항 타입/난이도/본문 */
    private String questionType;      // question_type (필수)
    private String difficulty;        // difficulty (필수)
    private String questionText;      // question_text (필수)
    private String explanation;       // explanation (선택)

    /** TEXT/INITIALS 등에서 사용하는 정답 텍스트 */
    private String textAnswer;        // text_answer (TEXT일 때 필수)

    /** 객관식 보기 + 정답 여부 플래그 */
    private String  choice1Text;
    private Boolean choice1IsAnswer;
    private String  choice2Text;
    private Boolean choice2IsAnswer;
    private String  choice3Text;
    private Boolean choice3IsAnswer;
    private String  choice4Text;
    private Boolean choice4IsAnswer;
}
