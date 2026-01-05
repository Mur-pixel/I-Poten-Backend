package com.cygnus.ipoten.quiz_session_scope.service;

import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz_session.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz_session.entity.enums.SessionSourceType;
import com.cygnus.ipoten.quiz_session.service.response.StartQuizSessionResponse;
import com.cygnus.ipoten.quiz_session_answer.service.QuizSessionAnswerService;
import com.cygnus.ipoten.quiz_session_scope.value_objects.ScopeCondition;
import com.cygnus.ipoten.quiz_session_scope.value_objects.SeedPolicy;
import com.cygnus.ipoten.quiz_session_scope.value_objects.SessionSource;
import com.cygnus.ipoten.quiz_session_scope.value_objects.SetScope;
import com.cygnus.ipoten.quiz_session_scope.value_objects.TermCategoryScope;
import com.cygnus.ipoten.quiz_set.entity.enums.QuizSetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.cygnus.ipoten.quiz_session.entity.enums.SessionSourceType.WRONG_NOTE;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizScopeServiceImpl implements QuizScopeService {

    private final QuizSessionAnswerService quizSessionAnswerService;
    private final QuizQuestionRepository quizQuestionRepository;
    private final WordbookScopeService wordbookScopeService;

    @Override
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public StartQuizSessionResponse startScopedSession(Long accountId, ScopeCondition condition, String customTitle) {

        String title = normalizeTitle(customTitle);

        if (condition == null) {
            throw new IllegalArgumentException("ScopeCondition은 필수입니다.");
        }

        log.info("[START] sourceType={}", condition.getSourceType());

        switch (condition.getSourceType()) {

            case WORDBOOK -> {
                if (condition.getWordbookScope() == null) {
                    throw new IllegalArgumentException("WORDBOOK 스코프가 없습니다.");
                }

                // WORDBOOK은 "빌드된 세트"를 사용 (기존 정책 유지)
                var built = wordbookScopeService.buildQuizSet(condition.getWordbookScope());

                SeedPolicy seed = (condition.getSeedPolicy() != null)
                        ? condition.getSeedPolicy()
                        : SeedPolicy.fromRaw(null, null);

                SeedMode mode = (seed.getSeedMode() != null) ? seed.getSeedMode() : SeedMode.AUTO;

                if (mode == SeedMode.FIXED && seed.getFixedSeed() == null) {
                    throw new IllegalArgumentException("FIXED seedMode에는 fixedSeed가 필요합니다.");
                }

                long seedValue = resolveSeedValue(mode, seed.getFixedSeed(), accountId);

                List<Long> ids = new ArrayList<>(built.getQuestionIds());
                Collections.shuffle(ids, new Random(seedValue));

                return quizSessionAnswerService.startFromQuizSet(
                        accountId,
                        built.getQuizSetId(),
                        ids,
                        mode,
                        seedValue,
                        title
                );
            }

            case TERM_CATEGORY -> {

                var s = condition.getTermCategoryScope();
                if (s == null) throw new IllegalArgumentException("TERM_CATEGORY 스코프가 없습니다.");

                var typeScope = s.getQuestionTypeScope();

                // 1) VO 기준 MIX 판별 (우선)
                boolean isMixByScope = (typeScope == null) || typeScope.isMix();

                // 2) 혹시 옛 데이터/요청이 typeRaw만 주는 경우 fallback
                String rawType = normalizeTypeRaw(s.getTypeRaw());
                boolean isMixByRaw = (rawType == null || "mix".equalsIgnoreCase(rawType));

                if (isMixByScope || isMixByRaw) {
                    return startFromCategoryMix(accountId, s, condition.getSeedPolicy(), title);
                }

                int take = Math.max(1, Math.min(100, s.getCount()));
                DifficultyLevel dl = (s.getDifficultyScope() == null) ? null : s.getDifficultyScope().forRepoOrNull();

                // qt: VO 우선, 없으면 raw fallback
                QuestionType qt = (typeScope != null)
                        ? typeScope.toEntityOrNull()
                        : toQuestionTypeOrNull(s.getTypeRaw());

                if (qt == null) {
                    return startFromCategoryMix(accountId, s, condition.getSeedPolicy(), title);
                }

                List<String> normalizedLabelKeys = normalizeLabelKeys(s.getLabelKeys());
                boolean hasLabels = !normalizedLabelKeys.isEmpty();

                SeedPolicy seed = (condition.getSeedPolicy() != null)
                        ? condition.getSeedPolicy()
                        : SeedPolicy.fromRaw(null, null);

                SeedMode mode = (seed.getSeedMode() != null) ? seed.getSeedMode() : SeedMode.AUTO;
                long seedValue = resolveSeedValue(mode, seed.getFixedSeed(), accountId);

                log.info("[TERM_CATEGORY] categoryId={}, take={}, dl={}, qt={}, labels={} (hasLabels={})",
                        s.getCategoryId(), take, dl, qt, normalizedLabelKeys, hasLabels);

                List<Long> candidates = hasLabels
                        ? quizQuestionRepository.findIdsByCategoryFiltersAndLabels(
                        s.getCategoryId(), dl, qt, true, normalizedLabelKeys
                )
                        : quizQuestionRepository.findIdsByCategoryFilters(
                        s.getCategoryId(), dl, qt
                );

                if (candidates.size() < take) {
                    throw new IllegalArgumentException("문항 수가 부족합니다. 요청=" + take + ", 확보=" + candidates.size());
                }

                log.info("[TERM_CATEGORY] candidates.size()={}", candidates.size());

                List<Long> picked = new ArrayList<>(candidates);
                Collections.shuffle(picked, new Random(seedValue));
                picked = picked.subList(0, take);

                SessionSource source = SessionSource.of(
                        SessionSourceType.TERM_CATEGORY,
                        s.getCategoryId(),
                        null // partType은 null로 두면 AnswerServiceImpl에서 질문 타입으로 유추 가능
                );

                return quizSessionAnswerService.startFromScope(
                        accountId,
                        source,
                        picked,
                        mode,
                        seedValue,
                        title
                );
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
                        title
                );
            }

            case WRONG_NOTE -> {
                var s = condition.getWrongNoteScope();
                if (s == null || s.questionIds() == null || s.questionIds().isEmpty()) {
                    throw new IllegalArgumentException("WRONG_NOTE questionIds가 비었습니다.");
                }

                List<Long> picked = s.questionIds().stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .limit(100)
                        .toList();

                if (picked.isEmpty()) {
                    throw new IllegalArgumentException("WRONG_NOTE questionIds가 유효하지 않습니다.");
                }

                List<Long> exist = quizQuestionRepository.findExistingIds(picked);
                if (exist.size() != picked.size()) {
                    throw new IllegalArgumentException("존재하지 않는 questionIds가 포함되어 있습니다.");
                }

                SessionSource source = SessionSource.wrongNote(accountId);

                // seed는 기록용으로만 사용 (질문 순서는 picked 그대로 전달)
                SeedPolicy seedPolicy = (condition.getSeedPolicy() != null) ? condition.getSeedPolicy() : SeedPolicy.fromRaw(null, null);
                SeedMode seedMode = (seedPolicy.getSeedMode() != null) ? seedPolicy.getSeedMode() : SeedMode.AUTO;
                long seedValue = resolveSeedValue(seedMode, seedPolicy.getFixedSeed(), accountId);

                return quizSessionAnswerService.startFromScope(
                        accountId,
                        source,
                        picked,
                        seedMode,
                        seedValue,
                        title
                );
            }

            default -> throw new IllegalStateException("지원하지 않는 SourceType: " + condition.getSourceType());
        }
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
            String customTitle
    ) {
        if (quizSetId == null) throw new IllegalArgumentException("quizSetId는 필수입니다.");

        int take = (count == null ? 10 : Math.max(1, Math.min(100, count)));
        DifficultyLevel dl = (level == null || level == DifficultyLevel.MIX) ? null : level;

        List<QuestionType> types = resolveTypes(typeRaw);
        boolean allTypes = (types == null || types.isEmpty());
        List<QuestionType> typesParam = allTypes ? List.of(QuestionType.CHOICE) : types;

        List<Long> candidates = quizQuestionRepository.findIdsBySetFilters(
                quizSetId, dl, allTypes, typesParam
        );

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("필터 조건에 맞는 문항이 없습니다. setId=" + quizSetId);
        }

        if (seedPolicy == null) seedPolicy = SeedPolicy.fromRaw(null, null);
        SeedMode mode = (seedPolicy.getSeedMode() != null) ? seedPolicy.getSeedMode() : SeedMode.AUTO;

        long seedValue;
        if (mode == SeedMode.FIXED) {
            if (seedPolicy.getFixedSeed() == null) {
                throw new IllegalArgumentException("FIXED seedMode에는 fixedSeed가 필요합니다.");
            }
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
                seedValue,
                customTitle
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StartQuizSessionResponse loadSessionForPlay(Long accountId, Long sessionId) {
        return quizSessionAnswerService.loadForPlay(accountId, sessionId);
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

    private static List<String> normalizeLabelKeys(List<String> labelKeys) {
        if (labelKeys == null) return List.of();
        return labelKeys.stream()
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
        return startFromCategoryMix(accountId, s, seedPolicy, null);
    }

    private StartQuizSessionResponse startFromCategoryMix(
            Long accountId,
            TermCategoryScope s,
            SeedPolicy seedPolicy,
            String customTitle
    ) {
        List<String> labelKeys = normalizeLabelKeys(s.getLabelKeys());

        int take = Math.max(1, Math.min(100, s.getCount()));
        DifficultyLevel dl = (s.getDifficultyScope() == null) ? null : s.getDifficultyScope().forRepoOrNull();

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

        List<Long> choicePool = new ArrayList<>(findIdsByCategory(categoryId, dl, QuestionType.CHOICE, labelKeys));
        List<Long> oxPool = new ArrayList<>(findIdsByCategory(categoryId, dl, QuestionType.OX, labelKeys));
        List<Long> initialsPool = new ArrayList<>(findIdsByCategory(categoryId, dl, QuestionType.INITIALS, labelKeys));

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

        SessionSource source = SessionSource.of(
                SessionSourceType.TERM_CATEGORY,
                categoryId,
                QuizSetType.MIX
        );

        return quizSessionAnswerService.startFromScope(
                accountId,
                source,
                picked,
                mode,
                seedValue,
                customTitle
        );
    }

    private int takeFrom(List<Long> pool, int n, List<Long> out) {
        int k = Math.min(n, pool.size());
        for (int i = 0; i < k; i++) out.add(pool.get(i));
        if (k > 0) pool.subList(0, k).clear();
        return k;
    }

    private List<Long> findIdsByCategory(
            Long categoryId,
            DifficultyLevel dl,
            QuestionType type,
            List<String> normalizedLabelKeys
    ) {
        boolean hasLabels = normalizedLabelKeys != null && !normalizedLabelKeys.isEmpty();

        return hasLabels
                ? quizQuestionRepository.findIdsByCategoryFiltersAndLabels(
                categoryId, dl, type, true, normalizedLabelKeys
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

    private static String normalizeTypeRaw(String raw) {
        if (raw == null) return null;
        String r = raw.trim();
        if (r.isBlank()) return null;

        String upper = r.toUpperCase();
        if (upper.contains("MIX")) return "mix";

        return r;
    }

    private static String normalizeTitle(String title) {
        if (title == null) return null;
        String s = title.trim();
        if (s.isBlank()) return null;
        return (s.length() > 40) ? s.substring(0, 40) : s;
    }
}
