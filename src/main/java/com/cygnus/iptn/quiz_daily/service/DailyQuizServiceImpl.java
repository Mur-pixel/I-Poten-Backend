package com.cygnus.iptn.quiz_daily.service;

import com.cygnus.iptn.quiz_daily.entity.enums.DailyStartMode;
import com.cygnus.iptn.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.iptn.quiz_question.entity.enums.QuestionType;
import com.cygnus.iptn.quiz_session.entity.enums.SeedMode;
import com.cygnus.iptn.quiz_session.entity.enums.SessionStatus;
import com.cygnus.iptn.quiz_session.repository.QuizSessionRepository;
import com.cygnus.iptn.quiz_session_scope.service.QuizScopeService;
import com.cygnus.iptn.quiz_session_scope.value_objects.*;
import com.cygnus.iptn.quiz_session.service.response.StartQuizSessionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
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
    @Transactional
    public DailyStarted startGeneralDaily(Long accountId, DailyStartMode mode) {

        LocalDate today = LocalDate.now(KST);
        LocalDate baseYmd = today;

        if (mode == null) mode = DailyStartMode.RESUME;

        if (mode == DailyStartMode.RESUME) {
            baseYmd = quizSessionRepository
                    .findTopByAccount_IdAndDailyIssueTypeAndSessionStatusInAndDeletedAtIsNullOrderByStartedAtDesc(
                            accountId, "GENERAL", List.of(SessionStatus.IN_PROGRESS)
                    )
                    .map(s -> s.getDailyYmd() != null
                            ? s.getDailyYmd()
                            : LocalDate.ofInstant(s.getStartedAt(), KST))
                    .orElse(today);

        } else if (mode == DailyStartMode.TODAY) {
            baseYmd = today;

            quizSessionRepository.expireDailyInProgress(accountId, baseYmd, "GENERAL", QuestionType.CHOICE.name());
            quizSessionRepository.expireDailyInProgress(accountId, baseYmd, "GENERAL", QuestionType.OX.name());
            quizSessionRepository.expireDailyInProgress(accountId, baseYmd, "GENERAL", QuestionType.INITIALS.name());
        }

        StartQuizSessionResponse choice   = startOne(accountId, baseYmd, QuestionType.CHOICE,   3, "DAILY_GENERAL:CHOICE");
        StartQuizSessionResponse ox       = startOne(accountId, baseYmd, QuestionType.OX,       3, "DAILY_GENERAL:OX");
        StartQuizSessionResponse initials = startOne(accountId, baseYmd, QuestionType.INITIALS, 3, "DAILY_GENERAL:INITIALS");

        boolean hasUnfinishedSession =
                !today.equals(baseYmd)
                        && quizSessionRepository.existsByAccount_IdAndDailyYmdAndDailyIssueTypeAndSessionStatusAndDeletedAtIsNull(
                        accountId, baseYmd, "GENERAL", SessionStatus.IN_PROGRESS
                );

        return new DailyStarted(today, baseYmd, choice, ox, initials, hasUnfinishedSession);
    }

    private StartQuizSessionResponse startOne(
            Long accountId,
            LocalDate ymd,
            QuestionType type,
            int count,
            String salt
    ) {

        // 1) IN_PROGRESS 우선
        var inProgress = quizSessionRepository
                .findTopByAccount_IdAndDailyYmdAndDailyIssueTypeAndDailyQuestionTypeAndSessionStatusInAndDeletedAtIsNullOrderByStartedAtDesc(
                        accountId, ymd, "GENERAL", type.name(), List.of(SessionStatus.IN_PROGRESS)
                );

        if (inProgress.isPresent()) {
            return quizScopeService.loadSessionForPlay(accountId, inProgress.get().getId());
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
            var again = quizSessionRepository
                    .findTopByAccount_IdAndDailyYmdAndDailyIssueTypeAndDailyQuestionTypeAndSessionStatusInAndDeletedAtIsNullOrderByStartedAtDesc(
                            accountId, ymd, "GENERAL", type.name(),
                            List.of(SessionStatus.IN_PROGRESS, SessionStatus.SUBMITTED, SessionStatus.EXPIRED)
                    );
            if (again.isPresent()) {
                return quizScopeService.loadSessionForPlay(accountId, again.get().getId());
            }
            throw dup;
        }
    }
}