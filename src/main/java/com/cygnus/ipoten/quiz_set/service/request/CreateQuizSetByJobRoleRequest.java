package com.cygnus.ipoten.quiz_set.service.request;

import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.job.enums.JobRole;
import lombok.Builder;
import lombok.Getter;

/**
 * 직무(JobRole) 기반 퀴즈 세트 생성 요청 DTO.
 * - 어떤 직무용 세트를 만들지 (jobRole)
 * - 몇 문항을 만들지 (count)
 * - 어떤 유형으로 출제할지 (questionType: CHOICE/OX/INITIALS/MIX)
 * - 난이도 (difficulty)
 *
 * 실제 구현은 QuizSetService.registerQuizSetByJobRole(...) 에서 사용.
 */
@Getter
@Builder
public class CreateQuizSetByJobRoleRequest {

    /** 대상 직무/역할 (예: GENERAL, FRONTEND, BACKEND ...) */
    private final JobRole jobRole;

    /** 생성할 문항 수 */
    private final int count;

    /** 출제 형태 (카테고리 기반 요청과 동일한 enum 재사용) */
    private final CreateQuizSetByCategoryRequest.QuestionType questionType;

    /** 난이도 (null 이면 서비스단에서 기본값 처리) */
    private final DifficultyLevel difficulty;
}