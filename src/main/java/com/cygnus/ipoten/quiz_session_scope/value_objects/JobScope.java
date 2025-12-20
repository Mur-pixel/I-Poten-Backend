package com.cygnus.ipoten.quiz_session_scope.value_objects;

import com.cygnus.ipoten.job.enums.JobRole;
import com.cygnus.ipoten.quiz_set.service.request.CreateQuizSetByJobRoleRequest;
import lombok.Value;

/**
 * 직무(Job Role) 기반 퀴즈 세션 생성을 위한 출제 범위(Scope)를 표현하는 도메인 객체.
 *
 * 이 클래스는 "특정 직무를 기준으로 어떤 조건의 문제를 몇 개 출제할 것인가"라는
 * 퀴즈 세션 생성 의도를 하나의 도메인 모델로 표현한다.
 *
 * 카테고리 단위가 아닌 직무(JobRole)를 기준으로 문제를 선별한다는 점에서
 * CategoryScope와 구분되며, 직무 추천 퀴즈나 직무 맞춤 학습 세션 생성에 사용된다.
 *
 * 구성 요소 의미:
 * - jobRole           : 출제 기준이 되는 직무 역할 (Backend, Frontend, Embedded 등)
 * - count             : 해당 직무 기준으로 출제할 문제 수 (1~100)
 * - questionTypeScope : 문제 유형 출제 범위 (단일 유형 또는 MIX)
 * - difficultyScope   : 난이도 출제 범위 (단일 난이도 또는 MIX)
 * - seedPolicy        : 문제 랜덤 순서를 결정하는 시드 정책
 *
 * 책임과 역할:
 * - 직무 기반 출제에 필요한 모든 조건을 하나의 객체로 응집
 * - 잘못된 출제 범위(직무 미지정, 문제 수 초과 등)를 생성 시점에 차단
 * - 외부 입력(String roleRaw)을 도메인 enum(JobRole)으로 변환
 * - 하위 애플리케이션 계층(CreateQuizSetByJobRoleRequest)으로 안전하게 변환
 *
 * 설계 의도:
 * - 직무 기반 퀴즈 생성 로직의 파라미터를 의미 있는 도메인 객체로 승격
 * - QuestionTypeScope, DifficultyScope, SeedPolicy와 조합되어
 *   "직무 맞춤 출제 정책"을 명확히 드러내는 모델 구성
 * - 서비스 계층에서는 JobScope 단위로만 판단하도록 책임 이동
 *
 * 사용 예:
 * - Backend 직무 기준 MIX 유형 + MIX 난이도 20문제 출제
 * - 고정 시드를 사용해 직무 퀴즈 세션을 재현 가능하게 생성
 *
 * 주의 사항:
 * - 이 객체는 직무 기반 출제 범위 정의에만 집중하며,
 *   실제 문제 선택 및 섞기 로직은 별도의 생성 서비스에서 수행된다.
 */
@Value
public class JobScope {
    JobRole jobRole;
    int count;
    QuestionTypeScope questionTypeScope;
    DifficultyScope difficultyScope;

    public JobScope(JobRole jobRole,
                    int count,
                    QuestionTypeScope questionTypeScope,
                    DifficultyScope difficultyScope
    ) {

        if (jobRole == null) {
            throw new IllegalArgumentException("jobRole은 필수입니다.");
        }

        if (count <= 0 || count > 100 ) {
            throw new IllegalArgumentException("JobScope: count는 1~100 사이여야 합니다.");
        }

        this.jobRole = jobRole;
        this.count = count;
        this.questionTypeScope = questionTypeScope;
        this.difficultyScope = difficultyScope;
    }

    public static JobScope fromRaw(String roleRaw,
                                   int count,
                                   QuestionTypeScope questionTypeScope,
                                   DifficultyScope difficultyScope
    ) {
        JobRole jobRole = JobRole.valueOf(roleRaw);
        return new JobScope(jobRole, count, questionTypeScope, difficultyScope);
    }

    public CreateQuizSetByJobRoleRequest toCreateQuizSetRequest() {
        return CreateQuizSetByJobRoleRequest.builder()
                .jobRole(jobRole)
                .count(count)
                .questionType(questionTypeScope.getType())
                .difficulty(difficultyScope.getLevel())
                .build();
    }
}
