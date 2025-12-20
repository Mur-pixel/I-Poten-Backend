package com.cygnus.ipoten.quiz_session_scope.service;

import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz_session.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz.service.response.BuiltQuizSetResponse;
import com.cygnus.ipoten.quiz_session.service.response.StartQuizSessionResponse;
import com.cygnus.ipoten.quiz_session_answer.service.QuizSessionAnswerService;
import com.cygnus.ipoten.quiz_session_scope.value_objects.ScopeCondition;
import com.cygnus.ipoten.quiz_session_scope.value_objects.SeedPolicy;
import com.cygnus.ipoten.quiz_session_scope.value_objects.SetScope;
import com.cygnus.ipoten.quiz_session_scope.value_objects.TermCategoryScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizScopeServiceImpl implements QuizScopeService {

    private final JobRoleScopeService jobRoleScopeService;
    private final QuizSessionAnswerService quizSessionAnswerService;
    private final QuizQuestionRepository quizQuestionRepository;
    private final WordbookScopeService wordbookScopeService;

    @Override
    @Transactional
    public StartQuizSessionResponse startScopedSession(Long accountId, ScopeCondition condition) {

        if (condition == null) {
            throw new IllegalArgumentException("ScopeCondition은 필수입니다.");
        }

        log.info("[START] sourceType={}", condition.getSourceType());

        BuiltQuizSetResponse built;

        switch (condition.getSourceType()) {

            case WORDBOOK -> {
                if (condition.getWordbookScope() == null) throw new IllegalArgumentException("WORDBOOK 스코프가 없습니다.");
                built = wordbookScopeService.buildQuizSet(condition.getWordbookScope());
            }

            case TERM_CATEGORY -> {

                var s = condition.getTermCategoryScope();
                if (s == null) throw new IllegalArgumentException("TERM_CATEGORY 스코프가 없습니다.");

                log.info("[TERM_CATEGORY] rawType='{}'", s.getTypeRaw());

                String rawType = (s.getTypeRaw() == null) ? null : s.getTypeRaw().trim();
                if (rawType == null || rawType.isBlank() || "mix".equalsIgnoreCase(rawType)) {
                    return startFromCategoryMix(accountId, s, condition.getSeedPolicy());
                }

                int take = Math.max(1, Math.min(100, s.getCount()));
                DifficultyLevel dl = (s.getDifficultyScope().getLevel() == null || s.getDifficultyScope().getLevel() == DifficultyLevel.MIX)
                        ? null : s.getDifficultyScope().getLevel();

                List<String> normalizedTagKeys = normalizeTagKeys(s.getTopicTagKeys());

                log.info("[TERM_CATEGORY] categoryId={}, take={}, typeRaw='{}', level={}, tags={}",
                        s.getCategoryId(), take, s.getTypeRaw(),
                        s.getDifficultyScope().getLevel(), normalizedTagKeys);

                // seed 준비(DAILY는 결정적으로) - (여기선 set pick에 직접 쓰진 않지만 유지)
                SeedPolicy seed = (condition.getSeedPolicy() != null) ? condition.getSeedPolicy() : SeedPolicy.fromRaw(null, null);
                SeedMode mode = (seed.getSeedMode() != null) ? seed.getSeedMode() : SeedMode.AUTO;
                long seedValue = resolveSeedValue(mode, seed.getFixedSeed(), accountId);

                // 기존 단일 타입 흐름 유지
                QuestionType qt = toQuestionTypeOrNull(s.getTypeRaw());

                Pageable one = PageRequest.of(0, 1);

                // ✅ tagKeys 비었으면 "노태그" 쿼리, 있으면 "태그" 쿼리
                var setIds = findEligibleSetIdsByCategory(
                        s.getCategoryId(),
                        qt,
                        dl,
                        normalizedTagKeys,
                        take,
                        one
                );

                if (setIds.isEmpty()) {
                    throw new IllegalArgumentException("조건에 맞는 퀴즈 세트가 없습니다. categoryId=" + s.getCategoryId());
                }

                Long pickedSetId = setIds.get(0);

                return startFromSet(
                        accountId,
                        pickedSetId,
                        take,
                        rawType,
                        s.getDifficultyScope().getLevel(),
                        condition.getSeedPolicy(),
                        s.getTopicTagKeys()
                );
            }

            case JOB -> {
                if (condition.getJobScope() == null) throw new IllegalArgumentException("Job 스코프가 없습니다.");
                built = jobRoleScopeService.buildQuizSet(condition.getJobScope());
            }

            case SET -> {
                SetScope ss = condition.getSetScope();
                if (ss == null) throw new IllegalArgumentException("SET 스코프가 없습니다.");

                return startFromSet(
                        accountId,
                        ss.getSetId(),
                        ss.getCount(),
                        ss.getTypeRaw(),
                        ss.getLevel(),
                        condition.getSeedPolicy(),
                        ss.getTopicTagKeys()
                );
            }

            default -> throw new IllegalStateException("지원하지 않는 SourceType: " + condition.getSourceType());
        }

        SeedPolicy seed = (condition.getSeedPolicy() != null) ? condition.getSeedPolicy() : SeedPolicy.fromRaw(null, null);
        SeedMode mode = (seed.getSeedMode() != null) ? seed.getSeedMode() : SeedMode.AUTO;

        if (mode == SeedMode.FIXED && seed.getFixedSeed() == null) {
            throw new IllegalArgumentException("FIXED seedMode에는 fixedSeed가 필요합니다.");
        }

        long seedValue = (mode == SeedMode.FIXED)
                ? seed.getFixedSeed()
                : ThreadLocalRandom.current().nextLong();

        List<Long> ids = new ArrayList<>(built.getQuestionIds());
        Collections.shuffle(ids, new Random(seedValue));

        return quizSessionAnswerService.startFromQuizSet(
                accountId,
                built.getQuizSetId(),
                ids,
                mode,
                seedValue
        );
    }

    @Override
    @Transactional
    public StartQuizSessionResponse startFromSet(
            Long accountId,
            Long quizSetId,
            Integer count,
            String typeRaw,
            DifficultyLevel level,
            SeedPolicy seedPolicy,
            List<String> tagKeys
    ) {
        if (quizSetId == null) throw new IllegalArgumentException("quizSetId는 필수입니다.");

        int take = (count == null ? 10 : Math.max(1, Math.min(100, count)));
        DifficultyLevel dl = (level == null || level == DifficultyLevel.MIX) ? null : level;

        List<QuestionType> types = resolveTypes(typeRaw);
        boolean allTypes = (types == null || types.isEmpty());
        List<QuestionType> typesParam = allTypes ? List.of(QuestionType.CHOICE) : types;

        List<String> normalizedTagKeys = normalizeTagKeys(tagKeys);
        boolean hasTags = !normalizedTagKeys.isEmpty();

        List<Long> candidates = hasTags
                ? quizQuestionRepository.findIdsBySetFiltersAndTopicTags(quizSetId, dl, allTypes, typesParam, true, normalizedTagKeys)
                : quizQuestionRepository.findIdsBySetFilters(quizSetId, dl, allTypes, typesParam);

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("필터 조건에 맞는 문항이 없습니다. setId=" + quizSetId);
        }

        if (seedPolicy == null) seedPolicy = SeedPolicy.fromRaw(null, null);
        SeedMode mode = (seedPolicy.getSeedMode() != null) ? seedPolicy.getSeedMode() : SeedMode.AUTO;

        long seedValue;
        if (mode == SeedMode.FIXED) {
            if (seedPolicy.getFixedSeed() == null) throw new IllegalArgumentException("FIXED seedMode에는 fixedSeed가 필요합니다.");
            seedValue = seedPolicy.getFixedSeed();
        } else {
            seedValue = ThreadLocalRandom.current().nextLong();
        }

        List<Long> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, new Random(seedValue));
        List<Long> picked = new ArrayList<>(shuffled.subList(0, Math.min(take, shuffled.size())));

        return quizSessionAnswerService.startFromQuizSet(
                accountId,
                quizSetId,
                picked,
                mode,
                seedValue
        );
    }

    private static List<QuestionType> resolveTypes(String raw) {
        if (raw == null || raw.isBlank() || "mix".equalsIgnoreCase(raw)) return List.of();
        return switch (raw.trim().toLowerCase()) {
            case "choice" -> List.of(QuestionType.CHOICE);
            case "ox" -> List.of(QuestionType.OX);
            case "initials" -> List.of(QuestionType.INITIALS);
            default -> List.of();
        };
    }

    private static List<String> normalizeTagKeys(List<String> tagKeys) {
        if (tagKeys == null) return List.of();
        return tagKeys.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase(java.util.Locale.ROOT))
                .distinct()
                .toList();
    }

    private static QuestionType toQuestionTypeOrNull(String raw) {
        if (raw == null || raw.isBlank() || "mix".equalsIgnoreCase(raw)) return null;
        return switch (raw.trim().toLowerCase()) {
            case "choice" -> QuestionType.CHOICE;
            case "ox" -> QuestionType.OX;
            case "initials" -> QuestionType.INITIALS;
            default -> null;
        };
    }

    private long resolveSeedValue(SeedMode mode, Long fixedSeed, Long accountId) {
        if (mode == null) return ThreadLocalRandom.current().nextLong();

        return switch (mode) {
            case FIXED -> {
                if (fixedSeed == null) throw new IllegalArgumentException("FIXED seedMode에는 fixedSeed가 필요합니다.");
                yield fixedSeed;
            }
            case DAILY -> {
                var zone = java.time.ZoneId.of("Asia/Seoul");
                var today = java.time.LocalDate.now(zone);
                yield java.util.Objects.hash(accountId, today);
            }
            default -> ThreadLocalRandom.current().nextLong();
        };
    }

    private static long mixSeed(long a, long b) {
        long x = a ^ (b + 0x9E3779B97F4A7C15L);
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    private StartQuizSessionResponse startFromCategoryMix(
            Long accountId,
            TermCategoryScope s,
            SeedPolicy seedPolicy
    ) {

        List<String> tagKeys = normalizeTagKeys(s.getTopicTagKeys());

        log.info("[MIX] ENTER categoryId={}, take={}, level={}, tags={}",
                s.getCategoryId(), s.getCount(), s.getDifficultyScope().getLevel(), tagKeys);

        int take = Math.max(1, Math.min(100, s.getCount()));

        DifficultyLevel dl = (s.getDifficultyScope().getLevel() == null
                || s.getDifficultyScope().getLevel() == DifficultyLevel.MIX)
                ? null : s.getDifficultyScope().getLevel();

        if (seedPolicy == null) seedPolicy = SeedPolicy.fromRaw(null, null);
        SeedMode mode = (seedPolicy.getSeedMode() != null) ? seedPolicy.getSeedMode() : SeedMode.AUTO;

        long seedValue;
        if (mode == SeedMode.FIXED) {
            if (seedPolicy.getFixedSeed() == null) throw new IllegalArgumentException("FIXED seedMode에는 fixedSeed가 필요합니다.");
            seedValue = seedPolicy.getFixedSeed();
        } else {
            seedValue = ThreadLocalRandom.current().nextLong();
        }

        Long categoryId = s.getCategoryId();
        int poolSize = Math.min(500, Math.max(50, take * 10));

        // ✅ 타입별 풀(태그 유/무 분기)
        List<Long> choicePool = new ArrayList<>(findIdsByCategory(categoryId, dl, QuestionType.CHOICE, tagKeys));
        List<Long> oxPool = new ArrayList<>(findIdsByCategory(categoryId, dl, QuestionType.OX, tagKeys));
        List<Long> initialsPool = new ArrayList<>(findIdsByCategory(categoryId, dl, QuestionType.INITIALS, tagKeys));

        choicePool = limit(choicePool, poolSize);
        oxPool = limit(oxPool, poolSize);
        initialsPool = limit(initialsPool, poolSize);

        if (choicePool.isEmpty() && oxPool.isEmpty() && initialsPool.isEmpty()) {
            throw new IllegalArgumentException("조건에 맞는 문항이 없습니다. categoryId=" + s.getCategoryId());
        }

        Collections.shuffle(choicePool, new Random(mixSeed(seedValue, 1)));
        Collections.shuffle(oxPool, new Random(mixSeed(seedValue, 2)));
        Collections.shuffle(initialsPool, new Random(mixSeed(seedValue, 3)));

        int each = take / 3;
        int needOx = each;
        int needInit = each;
        int needChoice = take - needOx - needInit;

        List<Long> picked = new ArrayList<>(take);

        int gotOx = takeFrom(oxPool, needOx, picked);
        int gotInit = takeFrom(initialsPool, needInit, picked);

        needChoice += (needOx - gotOx) + (needInit - gotInit);
        takeFrom(choicePool, needChoice, picked);

        if (picked.size() < take) takeFrom(choicePool, take - picked.size(), picked);
        if (picked.size() < take) takeFrom(oxPool, take - picked.size(), picked);
        if (picked.size() < take) takeFrom(initialsPool, take - picked.size(), picked);

        if (picked.size() < take) {
            throw new IllegalArgumentException("문항 수가 부족합니다. 요청=" + take + ", 확보=" + picked.size());
        }

        Collections.shuffle(picked, new Random(seedValue));

        // ✅ baseSetId도 태그 유/무 분기
        Long baseSetId = pickBaseSetIdForCategory(categoryId, dl, tagKeys);

        log.info("[MIX] pool sizes: choice={}, ox={}, initials={}",
                choicePool.size(), oxPool.size(), initialsPool.size());

        log.info("[MIX] picked type counts = {}",
                quizQuestionRepository.countTypesByIds(picked)
                        .stream()
                        .map(r -> r[0] + ":" + r[1])
                        .toList()
        );

        return quizSessionAnswerService.startFromQuizSet(
                accountId,
                baseSetId,
                picked,
                mode,
                seedValue
        );
    }

    private int takeFrom(List<Long> pool, int n, List<Long> out) {
        int k = Math.min(n, pool.size());
        for (int i = 0; i < k; i++) out.add(pool.get(i));
        if (k > 0) pool.subList(0, k).clear();
        return k;
    }

    private Long pickBaseSetIdForCategory(Long categoryId, DifficultyLevel dl, List<String> tagKeys) {
        Pageable one = PageRequest.of(0, 1);

        var choiceSet = findEligibleSetIdsByCategory(categoryId, QuestionType.CHOICE, dl, tagKeys, 1, one);
        if (!choiceSet.isEmpty()) return choiceSet.get(0);

        var anySet = findEligibleSetIdsByCategory(categoryId, null, dl, tagKeys, 1, one);
        if (!anySet.isEmpty()) return anySet.get(0);

        throw new IllegalArgumentException("categoryId=" + categoryId + " 에 해당하는 quizSet을 찾지 못했습니다.");
    }

    private List<Long> findEligibleSetIdsByCategory(
            Long categoryId,
            QuestionType type,
            DifficultyLevel dl,
            List<String> normalizedTagKeys,
            long minCount,
            Pageable pageable
    ) {
        boolean hasTags = normalizedTagKeys != null && !normalizedTagKeys.isEmpty();

        return hasTags
                ? quizQuestionRepository.findEligibleSetIdsByCategoryAndTopicTags(
                categoryId, type, dl, true, normalizedTagKeys, minCount, pageable
        )
                : quizQuestionRepository.findEligibleSetIdsByCategory(
                categoryId, type, dl, minCount, pageable
        );
    }

    private List<Long> findIdsByCategory(
            Long categoryId,
            DifficultyLevel dl,
            QuestionType type,
            List<String> normalizedTagKeys
    ) {
        boolean hasTags = normalizedTagKeys != null && !normalizedTagKeys.isEmpty();

        return hasTags
                ? quizQuestionRepository.findIdsByCategoryFiltersAndTopicTags(
                categoryId, dl, type, true, normalizedTagKeys
        )
                : quizQuestionRepository.findIdsByCategoryFilters(
                categoryId, dl, type
        );
    }

    private static <T> List<T> limit(List<T> list, int max) {
        if (list == null || list.isEmpty()) return List.of();
        int end = Math.min(max, list.size());
        return new ArrayList<>(list.subList(0, end));
    }
}
