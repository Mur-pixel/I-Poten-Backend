package com.cygnus.ipoten.quiz_daily.service;

import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_session.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus;
import com.cygnus.ipoten.quiz_session.repository.QuizSessionRepository;
import com.cygnus.ipoten.quiz_session_scope.service.QuizScopeService;
import com.cygnus.ipoten.quiz_session_scope.value_objects.*;
import com.cygnus.ipoten.quiz_session.service.response.StartQuizSessionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyQuizServiceImpl implements DailyQuizService {

    private final DailySeedService dailySeedService;
    private final QuizScopeService quizScopeService;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizSessionDailyWriter quizSessionDailyWriter;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final List<DailyPool> GENERAL_POOLS = List.of(
            new DailyPool(12L), // js
            new DailyPool(11L), // html
            new DailyPool(16L), // a11y
            new DailyPool(97L), // ts
            new DailyPool(24L), // sql
            new DailyPool(31L), // rest
            new DailyPool(64L), // devops
            new DailyPool(18L), // sec
            new DailyPool(14L)  // framework 묶음
    );


    private record DailyPool(Long termCategoryId) {}

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DailyStarted startGeneralDaily(Long accountId) {
        LocalDate ymd = LocalDate.now(KST);

        StartQuizSessionResponse choice    = startOne(accountId, ymd, QuestionType.CHOICE,   3, "DAILY_GENERAL:CHOICE");
        StartQuizSessionResponse ox        = startOne(accountId, ymd, QuestionType.OX,       3, "DAILY_GENERAL:OX");
        StartQuizSessionResponse initials  = startOne(accountId, ymd, QuestionType.INITIALS, 3, "DAILY_GENERAL:INITIALS");

        return new DailyStarted(ymd, choice, ox, initials);
    }

    private StartQuizSessionResponse startOne(
            Long accountId,
            LocalDate ymd,
            QuestionType type,
            int count,
            String salt
    ) {
        var statuses = List.of(SessionStatus.IN_PROGRESS);

        var existing = quizSessionRepository
                .findTopByAccount_IdAndDailyYmdAndDailyIssueTypeAndDailyQuestionTypeAndSessionStatusInOrderByStartedAtDesc(
                        accountId, ymd, "GENERAL", type.name(), statuses
                );

        if (existing.isPresent()) {
            return quizScopeService.loadSessionForPlay(accountId, existing.get().getId());
        }

        try {
            // 풀 선택 seed(유저/날짜/타입별로 고정)
            long poolSeed = dailySeedService.resolveSeed(SeedMode.DAILY, accountId, ymd, null, salt + ":POOL");
            int startIdx = Math.floorMod(poolSeed, GENERAL_POOLS.size());

            // 실제 문항 셔플/보기 배치 seed (세션 생성은 FIXED)
            long fixedSeed = dailySeedService.resolveSeed(SeedMode.DAILY, accountId, ymd, null, salt);
            SeedPolicy seedPolicy = SeedPolicy.fromRaw("FIXED", fixedSeed);

            QuestionTypeScope questionTypeScope = QuestionTypeScope.fromRaw(type.name());
            DifficultyScope difficultyScope = DifficultyScope.of(DifficultyLevel.MIX);

            // 풀 부족하면 다음 풀로 순차 fallback (최대 pools.size()번)
            IllegalArgumentException last = null;

            for (int step = 0; step < GENERAL_POOLS.size(); step++) {
                DailyPool pool = GENERAL_POOLS.get((startIdx + step) % GENERAL_POOLS.size());

                TermCategoryScope termCategoryScope = new TermCategoryScope(
                        pool.termCategoryId(),
                        count,
                        questionTypeScope,
                        difficultyScope,
                        List.of() // labelKeys는 일단 비움
                );

                ScopeCondition condition = ScopeCondition.forCategory(termCategoryScope, seedPolicy);

                String title = "[오늘의 퀴즈] " + type.displayName() + " " + count + "문제";

                try {
                    StartQuizSessionResponse created = quizScopeService.startScopedSession(accountId, condition, title);
                    quizSessionDailyWriter.markDaily(created.getSessionId(), accountId, ymd, "GENERAL", type.name());
                    return created;
                } catch (IllegalArgumentException e) {
                    // "문항 수 부족" / "필터 조건 없음" 등일 때 다음 풀로 넘어감
                    last = e;
                }
            }

            throw (last != null) ? last : new IllegalArgumentException("데일리 풀에서 문항을 만들 수 없습니다.");

        } catch (DataIntegrityViolationException dup) {
            // UNIQUE 충돌(동시 요청) -> 다시 조회 후 반환
            var again = quizSessionRepository
                    .findTopByAccount_IdAndDailyYmdAndDailyIssueTypeAndDailyQuestionTypeAndSessionStatusInOrderByStartedAtDesc(
                            accountId, ymd, "GENERAL", type.name(), statuses
                    );
            if (again.isPresent()) {
                return quizScopeService.loadSessionForPlay(accountId, again.get().getId());
            }
            throw dup;
        }
    }
}
