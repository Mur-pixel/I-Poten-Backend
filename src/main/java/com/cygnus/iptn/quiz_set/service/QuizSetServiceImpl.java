package com.cygnus.iptn.quiz_set.service;

import com.cygnus.iptn.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.iptn.quiz_set.entity.QuizSetQuestion;
import com.cygnus.iptn.quiz_set.repository.QuizSetQuestionRepository;
import com.cygnus.iptn.quiz_set.service.request.CreateQuizSetByWordbookRequest;
import com.cygnus.iptn.quiz_question.entity.QuizChoice;
import com.cygnus.iptn.quiz_question.entity.QuizQuestion;
import com.cygnus.iptn.quiz_set.entity.QuizSet;
import com.cygnus.iptn.quiz_question.entity.enums.QuestionType;
import com.cygnus.iptn.quiz_set.entity.enums.QuizSetType;
import com.cygnus.iptn.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.iptn.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.iptn.quiz_set.repository.QuizSetRepository;
import com.cygnus.iptn.quiz_analytics.service.RecentUsageService;
import com.cygnus.iptn.quiz_set.service.request.CreateQuizSetByCategoryRequest;
import com.cygnus.iptn.quiz_set.service.response.BuiltQuizSetResponse;
import com.cygnus.iptn.quiz_set.service.response.CreateQuizSetByCategoryResponse;
import com.cygnus.iptn.term_category.entity.TermCategory;
import com.cygnus.iptn.term.entity.Term;
import com.cygnus.iptn.term_category.repository.TermCategoryRepository;
import com.cygnus.iptn.wordbook.service.WordbookQueryService;
import com.cygnus.iptn.wordbook_term.repository.WordbookTermRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizSetServiceImpl implements QuizSetService {

    private final TermCategoryRepository termCategoryRepository;
    private final QuizSetRepository quizSetRepository;
    private final QuizSetQuestionRepository quizSetQuestionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final WordbookQueryService wordbookQueryService;
    private final QuizChoiceRepository quizChoiceRepository;
    private final WordbookTermRepository wordbookTermRepository;

    /** 선택 주입: 있으면 사용(최근 옵션 텍스트 재사용 회피 등), 없으면 SessionAnswerRepository로 폴백 */
    @Autowired(required = false)
    private RecentUsageService recentUsageService;

    @PersistenceContext
    private EntityManager em;

    /**
     * 세트 생성 + 문항 ID까지 응답(프론트에서 session 시작용으로 쓰기 좋음)
     */
    @Override
    @Transactional
    public BuiltQuizSetResponse registerQuizSetByCategoryReturningQuestions(CreateQuizSetByCategoryRequest request) {

        // 1) 카테고리 로드(있는 경우)
        TermCategory termCategory = null;
        if (request.getCategoryId() != null) {
            termCategory = termCategoryRepository.getReferenceById(request.getCategoryId());
        }

        // 2) 타이틀 확정
        String title = request.getTitle();
        if (title == null || title.isBlank()) {
            String cat = (termCategory != null && termCategory.getName() != null) ? termCategory.getName() : "카테고리";
            String ts  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            title = cat + " 퀴즈 - " + ts;
        }

        // 3) 세트 생성/저장
        QuizSetType setPart;
        var reqTypeCat = request.getQuestionType();
        if (reqTypeCat == null) {
            setPart = QuizSetType.CHOICE;
        } else {
            switch (reqTypeCat) {
                case INITIALS -> setPart = QuizSetType.INITIALS;
                case OX       -> setPart = QuizSetType.OX;
                case CHOICE   -> setPart = QuizSetType.CHOICE;
                case MIX      -> setPart = QuizSetType.CHOICE;
                default       -> setPart = QuizSetType.CHOICE;
            }
        }

        QuizSet quizSet = QuizSet.create(title);
        quizSetRepository.save(quizSet);

        // 4) 문제로 사용할 용어 선별
        List<Term> pickedTerms = pickTermsByCategoryPolicy(
                request.getCategoryId(),
                request.getCount()
        );
        if (pickedTerms.isEmpty()) {
            throw new IllegalStateException("해당 카테고리에 출제할 용어가 없습니다.");
        }

        // 5) QuizQuestion 생성/저장
        List<QuizQuestion> questions = new ArrayList<>(pickedTerms.size());
        for (Term term : pickedTerms) {
            QuestionType qType = resolveQuestionType(request.getQuestionType(), term);
            DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

            QuizQuestion q;
            if (qType == QuestionType.INITIALS) {
                q = new QuizQuestion(
                        term,
                        termCategory,
                        QuestionType.INITIALS,
                        difficulty,
                        makeQuestionText(term, QuestionType.INITIALS), // 문제 텍스트에만 초성 힌트 포함
                        koreanHead(term.getTitle()),                   // 정답은 '원 단어'
                        null
                );
            } else {
                q = new QuizQuestion(
                        term,
                        termCategory,
                        qType,
                        difficulty,
                        makeQuestionText(term, qType)
                );
            }
            questions.add(q);
        }
        quizQuestionRepository.saveAll(questions);

        // 6) 보기 생성 (카테고리 풀 기반)
        List<Term> pool = buildPoolForCategory(request.getCategoryId(), pickedTerms);
        createChoicesForQuestions(questions, pool);

        // 7) 질문 ID 조회
        quizQuestionRepository.saveAll(questions);
        em.flush(); // id 확정(안전하게)

        List<QuizSetQuestion> links = new ArrayList<>();
        for (QuizQuestion q : questions) {
            links.add(QuizSetQuestion.create(quizSet, q));
        }
        quizSetQuestionRepository.saveAll(links);

        List<Long> questionIds = questions.stream()
                .map(QuizQuestion::getId)
                .toList();

        // 8) 결과 반환
        return BuiltQuizSetResponse.builder()
                .quizSetId(quizSet.getId())
                .questionIds(questionIds)
                .title(quizSet.getTitle())
                .totalQuestions(questionIds.size())
                .build();
    }

    @Override
    @Transactional
    public BuiltQuizSetResponse registerQuizSetByWordbookReturningQuestions(CreateQuizSetByWordbookRequest request) {

        // 0) 입력 검증
        if (request == null || request.getAccountId() == null || request.getWordbookId() == null) {
            throw new IllegalArgumentException("계정 또는 폴더 식별자가 없습니다.");
        }
        if (request.getCount() <= 0) {
            throw new IllegalArgumentException("문항 수가 올바르지 않습니다.");
        }

        // 1) 폴더 소유권 확인
        boolean owned = wordbookQueryService.existsByIdAndAccountId(
                request.getWordbookId(), request.getAccountId()
        );
        if (!owned) {
            throw new SecurityException("폴더가 없거나 권한이 없습니다.");
        }

        // 2) 폴더 용어 조회
        List<Long> candidateTermIds =
                wordbookTermRepository.findDistinctTermIdsByWordbookAndAccountOrderByTermIdAsc(
                        request.getWordbookId(), request.getAccountId());

        if (candidateTermIds.isEmpty()) {
            // 폴더가 비었을 때만 400
            throw new IllegalArgumentException("폴더 내에 학습할 용어가 없습니다.");
        }

        // 요청 개수와 보유 개수 중 더 작은 값으로 클램프
        int targetCount = Math.min(Math.max(1, request.getCount()), candidateTermIds.size());

        // 3) 타이틀 확정
        String title = request.getTitle();
        if (title == null || title.isBlank()) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            title = "[SpoonNote] Folder#" + request.getWordbookId() + " - " + ts;
        }

        // 4) 세트 저장
        QuizSetType setPart;
        var reqTypeFold = request.getQuestionType();
        if (reqTypeFold == null) {
            setPart = QuizSetType.CHOICE;
        } else {
            switch (reqTypeFold) {
                case INITIALS -> setPart = QuizSetType.INITIALS;
                case OX       -> setPart = QuizSetType.OX;
                case CHOICE   -> setPart = QuizSetType.CHOICE;
                case MIX      -> setPart = QuizSetType.CHOICE;
                default       -> setPart = QuizSetType.CHOICE;
            }
        }

        QuizSet quizSet = QuizSet.create(title);
        quizSetRepository.save(quizSet);

        // 5) Term 엔티티 로드 + 폴더 내 순서 보존
        List<Term> loaded = em.createQuery(
                        "select t from Term t where t.id in :ids", Term.class)
                .setParameter("ids", candidateTermIds)
                .getResultList();

        Map<Long, Term> termById = loaded.stream()
                .collect(Collectors.toMap(Term::getId, t -> t));

        List<Term> pool = candidateTermIds.stream()
                .map(termById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (pool.isEmpty()) {
            throw new IllegalStateException("폴더에 매칭되는 용어 엔티티가 없습니다.");
        }

        List<Term> picked = pool.subList(0, Math.min(targetCount, pool.size()));

        // 6) 문항 생성/저장
        List<QuizQuestion> questions = new ArrayList<>(picked.size());
        for (Term term : picked) {
            QuestionType questionType = mixByTerm(term);

            var reqType2 = request.getQuestionType();
            if (reqType2 != null) {
                switch (reqType2) {
                    case CHOICE   -> questionType = QuestionType.CHOICE;
                    case OX       -> questionType = QuestionType.OX;
                    case INITIALS -> questionType = gateInitialsByKorean(term, QuestionType.INITIALS);
                    case MIX      -> questionType = mixByTerm(term);
                    default       -> questionType = mixByTerm(term);
                }
            }

            DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

            QuizQuestion q;
            if (questionType == QuestionType.INITIALS) {
                q = new QuizQuestion(
                        term,
                        null,
                        QuestionType.INITIALS,
                        difficulty,
                        makeQuestionText(term, QuestionType.INITIALS),
                        koreanHead(term.getTitle()),
                        null
                );
            } else {
                q = new QuizQuestion(
                        term,
                        null,
                        questionType,
                        difficulty,
                        makeQuestionText(term, questionType)
                );
            }
            questions.add(q);
        }

        // 6) 문항 생성/저장
        quizQuestionRepository.saveAll(questions);

        // 6-1) 보기 생성  ← 추가
        createChoicesForQuestions(questions, pool);

        em.flush();

        List<QuizSetQuestion> links = new ArrayList<>();
        for (QuizQuestion q : questions) {
            links.add(QuizSetQuestion.create(quizSet, q));
        }
        quizSetQuestionRepository.saveAll(links);


        List<Long> questionIds = questions.stream()
                .map(QuizQuestion::getId)
                .toList();

        return BuiltQuizSetResponse.builder()
                .quizSetId(quizSet.getId())
                .questionIds(questionIds)
                .title(quizSet.getTitle())
                .totalQuestions(questionIds.size())
                .build();
    }

    /**
     * 카테고리에서 문항 수 만큼 용어를 선별.
     */
    private List<Term> pickTermsByCategoryPolicy(Long categoryId, int count) {
        int targetCount = Math.max(1, Math.min(100, count));

        if (categoryId != null) {
            return em.createQuery(
                    "select t from Term t where t.termCategory.id = :cid order by t.id asc",
                    Term.class
            )
                    .setParameter("cid", categoryId)
                    .setMaxResults(targetCount)
                    .getResultList();
        } else {
            return em.createQuery(
                    "select t from Term t order by function('rand')",
                    Term.class
            )
                    .setMaxResults(targetCount)
                    .getResultList();
        }
    }

    /**
     * 요청 DTO의 QuestionType(MIX/CHOICE/OX/INITIAL) → 엔티티 QuestionType(CHOICE/OX/INITIALS) 매핑.
     * MIX는 termId 기반의 안정적 분배(CHOICE/OX/INITIALS)로 처리.
     */
    private QuestionType resolveQuestionType(CreateQuizSetByCategoryRequest.QuestionType reqType, Term term) {
        QuestionType baseType;
        if (reqType == null) {
            baseType = QuestionType.CHOICE;
        } else {
            switch (reqType) {
                case CHOICE -> baseType = QuestionType.CHOICE;
                case OX     -> baseType = QuestionType.OX;
                case INITIALS -> baseType = QuestionType.INITIALS;
                case MIX -> {
                    long seed = (term != null && term.getId() != null) ? term.getId() : System.nanoTime();
                    int slot = (int) (Math.abs(seed) % 3);
                    baseType = (slot == 0) ? QuestionType.CHOICE
                            : (slot == 1) ? QuestionType.OX
                            : QuestionType.INITIALS;
                }
                default -> baseType = QuestionType.CHOICE;
            }
        }
        return gateInitialsByKorean(term, baseType);
    }

    /** 문제 텍스트 생성 */
    private String makeQuestionText(Term term, QuestionType qType) {
        String title = (term != null && term.getTitle() != null) ? term.getTitle() : "제목 없음";
        String desc  = (term != null && term.getDescription() != null) ? term.getDescription() : "";

        switch (qType) {
            case INITIALS: {
                String hint = toKoreanInitials(koreanHead(title));
                String brief = oneLine(desc, 140); // 개행/중복공백 제거 + 길이 제한
                if (brief.isBlank()) brief = "~.";
                // 원하는 출력 형식:
                // 초성 힌트: ㅍㅇㅈ ㅂㅈ
                // 설명: ~.
                return "초성 힌트: " + hint + "\n설명: " + brief;
            }
            case OX:
                return (desc.isBlank() ? title : desc);
            case CHOICE:
            default:
                return (desc.isBlank() ? ("다음 설명에 해당하는 용어는? - " + title) : desc);
        }
    }

    /* ---------- 한글 초성 유틸 ---------- */

    private static final char HANGUL_BASE = 0xAC00;   // '가'
    private static final char HANGUL_END  = 0xD7A3;   // '힣'
    private static final char[] CHOSEONG = {
            'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ',
            'ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
    };

    private String toKoreanInitials(String s) {
        if (s == null || s.isBlank()) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= HANGUL_BASE && ch <= HANGUL_END) {
                int syllableIndex = ch - HANGUL_BASE;
                int choIndex = syllableIndex / (21 * 28);
                sb.append(CHOSEONG[choIndex]);
            } else {
                sb.append(ch); // 한글 외 문자는 그대로
            }
        }
        return sb.toString();
    }

    private QuestionType mixByTerm(Term term) {
        long seed = (term != null && term.getId() != null) ? term.getId() : System.nanoTime();
        int slot = (int) (Math.abs(seed) % 3);
        QuestionType t = (slot == 0) ? QuestionType.CHOICE
                : (slot == 1) ? QuestionType.OX
                : QuestionType.INITIALS;
        return gateInitialsByKorean(term, t);
    }

    /** 선택지(보기) 생성: 정의→용어(정답) + 같은 풀에서 오답 3개 */
    private void createChoicesForQuestions(List<QuizQuestion> questions, List<Term> distractorPool) {
        if (questions == null || questions.isEmpty()) return;

        for (QuizQuestion q : questions) {
            Term term = q.getTerm();
            if (term == null) continue;

            QuestionType type = q.getQuestionType();

            // INITIALS(초성) 타입은 텍스트 정답형이므로 보기 없음
            if (type == QuestionType.INITIALS) {
                continue;
            }

            // OX 문제: 보기 두 개 고정
            if (type == QuestionType.OX) {
                QuizChoice o = QuizChoice.create(q, "O", true);
                QuizChoice x = QuizChoice.create(q, "X", false);
                quizChoiceRepository.saveAll(List.of(o, x));
                continue;
            }

            // === CHOICE ===
            String correctText = safeText(term.getTitle());

            // 1) 1차 후보: 주어진 풀(폴더 전체/카테고리 전체 등)
            List<Term> candidates = distractorPool.stream()
                    .filter(t -> !Objects.equals(t.getId(), term.getId()))
                    .collect(Collectors.toList());
            Collections.shuffle(candidates);

            LinkedHashSet<String> used = new LinkedHashSet<>();
            used.add(normalize(correctText));

            List<QuizChoice> toSave = new ArrayList<>();

            // (정답)
            QuizChoice ans = QuizChoice.create(q, correctText, true);
            toSave.add(ans);

            // (오답) 1차: 풀에서 다른 용어 이름을 뽑아 3개까지 채우기
            for (Term d : candidates) {
                if (toSave.size() >= 4) break;
                String txt = safeText(d.getTitle());
                String norm = normalize(txt);
                if (norm.isBlank() || used.contains(norm)) continue;
                used.add(norm);

                QuizChoice c = QuizChoice.create(q, txt, false);
                toSave.add(c);
            }

            // 2) 폴백: 그래도 4지 못 채우면 DB에서 추가 샘플
            if (toSave.size() < 4) {
                List<Term> extra = em.createQuery(
                                "select t from Term t where t.id <> :id order by function('rand')",
                                Term.class
                        ).setParameter("id", term.getId())
                        .setMaxResults(16) // 넉넉히 뽑아 중복/공백 제거
                        .getResultList();

                for (Term d : extra) {
                    if (toSave.size() >= 4) break;
                    String txt = safeText(d.getTitle());
                    String norm = normalize(txt);
                    if (norm.isBlank() || used.contains(norm)) continue;
                    used.add(norm);

                    QuizChoice c = QuizChoice.create(q, txt, false);
                    toSave.add(c);
                }
            }

            // 3) 최종 방어: 여전히 부족하면(데이터 희소) 현재 개수로 진행하되 경고
            if (toSave.size() < 4 && type == QuestionType.CHOICE) {
                log.warn("[choices] CHOICE 4지 미만({}) → 데이터 희소. qId={}", toSave.size(), q.getId());
            }

            // 4) 섞기 + 저장 (별도 sortOrder 컬럼 없음)
            Collections.shuffle(toSave);
            quizChoiceRepository.saveAll(toSave);
        }

        em.flush();
    }

    private static String safeText(String s) { return (s == null ? "" : s.trim()); }
    private static String normalize(String s) {
        return (s == null) ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    /** 카테고리 기준 풀 만들기 (카테고리 없으면 질문에 딸린 term만 모아서 대체) */
    private List<Term> buildPoolForCategory(Long termCategoryId, List<Term> pickedTerms) {
        if (termCategoryId == null) return pickedTerms;
        return em.createQuery("select t from Term t where t.termCategory.id = :cid", Term.class)
                .setParameter("cid", termCategoryId)
                .getResultList();
    }

    private static final char HANGUL_SYLLABLES_BEGIN = '\uAC00'; // 가
    private static final char HANGUL_SYLLABLES_END   = '\uD7A3'; // 힣
    private static final char HANGUL_JAMO_BEGIN      = '\u3131'; // ㄱ
    private static final char HANGUL_JAMO_END        = '\u318E'; // ㆎ

    /** "쿠키(Cookie)" 같은 혼합 표기는 괄호 앞 한글만 남기고 검사 */
    private String koreanHead(String s) {
        if (s == null) return "";
        int p = s.indexOf('(');
        if (p > 0) s = s.substring(0, p);
        return s.trim();
    }

    /** 공백/구분자 제거 후 남는 문자가 모두 한글(완성형/자모)일 때만 true */
    private boolean isKoreanWord(String s) {
        if (s == null) return false;
        String core = koreanHead(s).replaceAll("[\\s\\-_/·ㆍ·]+", "");
        if (core.isEmpty()) return false;

        for (int i = 0; i < core.length(); i++) {
            char c = core.charAt(i);
            boolean syllable = (c >= HANGUL_SYLLABLES_BEGIN && c <= HANGUL_SYLLABLES_END);
            boolean jamo     = (c >= HANGUL_JAMO_BEGIN      && c <= HANGUL_JAMO_END);
            if (!(syllable || jamo)) return false;
        }
        return true;
    }

    /** INITIALS가 비한글 정답이면 CHOICE로 폴백 */
    private QuestionType gateInitialsByKorean(Term term, QuestionType t) {
        if (t == QuestionType.INITIALS) {
            String title = (term != null ? term.getTitle() : null);
            if (!isKoreanWord(title)) return QuestionType.CHOICE;
        }
        return t;
    }

    /** 여러 줄 설명을 한 줄로 정리하고 길이 제한 */
    private static String oneLine(String s, int maxLen) {
        if (s == null) return "";
        String t = s.replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (t.length() > maxLen) {
            t = t.substring(0, Math.max(0, maxLen - 1)) + "…";
        }
        return t;
    }

}
