package com.cygnus.ipoten.quiz.service;

import com.cygnus.ipoten.quiz.entity.Quiz;
import com.cygnus.ipoten.quiz.repository.QuizRepository;
import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz.entity.QuizSet;
import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz.repository.QuizSetRepository;
import com.cygnus.ipoten.quiz_analytics.service.RecentUsageService;
import com.cygnus.ipoten.quiz_session.repository.SessionAnswerRepository;
import com.cygnus.ipoten.quiz.service.generator.AutoQuizGenerator;
import com.cygnus.ipoten.quiz_session.service.request.CreateQuizSessionRequest;
import com.cygnus.ipoten.quiz.service.request.CreateQuizSetByCategoryRequest;
import com.cygnus.ipoten.quiz.service.request.CreateQuizSetByFolderRequest;
import com.cygnus.ipoten.quiz.service.response.BuiltQuizSetResponse;
import com.cygnus.ipoten.quiz_session.service.response.CreateQuizSessionResponse;
import com.cygnus.ipoten.quiz.service.response.CreateQuizSetByCategoryResponse;
import com.cygnus.ipoten.term_category.entity.TermCategory;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term_category.repository.TermCategoryRepository;
import com.cygnus.ipoten.wordbook.repository.WordbookTermRepository;
import com.cygnus.ipoten.wordbook.service.WordbookFolderQueryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    private final QuizQuestionRepository quizQuestionRepository;
    private final AutoQuizGenerator autoQuizGenerator;
    private final WordbookFolderQueryService wordbookFolderQueryService;
    private final SessionAnswerRepository sessionAnswerRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final WordbookTermRepository wordbookTermRepository;
    private final QuizRepository quizRepository;

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

        Quiz quiz = Quiz.create(title, setPart);
        quiz = quizRepository.save(quiz);

        QuizSet quizSet = QuizSet.create(quiz, title, setPart, termCategory);
        quizSetRepository.save(quizSet);

        // 4) 문제로 사용할 용어 선별
        List<Term> pickedTerms = pickTermsByCategoryPolicy(
                request.getCategoryId(),
                request.getCount(),
                request.isRandom()
        );
        if (pickedTerms.isEmpty()) {
            throw new IllegalStateException("해당 카테고리에 출제할 용어가 없습니다.");
        }

        // 5) QuizQuestion 생성/저장
        List<QuizQuestion> questions = new ArrayList<>(pickedTerms.size());
        int order = 0;
        for (Term term : pickedTerms) {
            QuestionType qType = resolveQuestionType(request.getQuestionType(), term);

            QuizQuestion q;
            if (qType == QuestionType.INITIALS) {
                q = QuizQuestion.textAnswer(
                        term, termCategory, QuestionType.INITIALS,
                        makeQuestionText(term, qType),
                        quizSet, toKoreanInitials(koreanHead(term.getTitle()))
                );
            } else {
                q = new QuizQuestion(
                        term, termCategory, qType,
                        makeQuestionText(term, qType),
                        quizSet
                );
            }
            questions.add(q);
        }
        quizQuestionRepository.saveAll(questions);

        // 6) 보기 생성 (카테고리 풀 기반)
        List<Term> pool = buildPoolForCategory(request.getCategoryId(), pickedTerms);
        createChoicesForQuestions(questions, pool);

        // 7) 질문 ID 조회
        List<Long> questionIds = em.createQuery(
                "select q.id from QuizQuestion q where q.quizSet.id = :sid order by q.id",
                Long.class
        ).setParameter("sid", quizSet.getId()).getResultList();

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
    public BuiltQuizSetResponse registerQuizSetByFolderReturningQuestions(CreateQuizSetByFolderRequest request) {

        // 0) 입력 검증
        if (request == null || request.getAccountId() == null || request.getFolderId() == null) {
            throw new IllegalArgumentException("계정 또는 폴더 식별자가 없습니다.");
        }
        if (request.getCount() <= 0) {
            throw new IllegalArgumentException("문항 수가 올바르지 않습니다.");
        }

        // 1) 폴더 소유권 확인
        boolean owned = wordbookFolderQueryService.existsByIdAndAccountId(
                request.getFolderId(), request.getAccountId()
        );
        if (!owned) {
            throw new SecurityException("폴더가 없거나 권한이 없습니다.");
        }

        // 2) 폴더 용어 조회
        List<Long> candidateTermIds =
                wordbookTermRepository.findDistinctTermIdsByFolderAndAccountOrderByTermIdAsc(
                        request.getFolderId(), request.getAccountId());

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
            title = "[SpoonNote] Folder#" + request.getFolderId() + " - " + ts;
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
        Quiz quiz = Quiz.create(title, setPart);
        quiz = quizRepository.save(quiz);

        QuizSet quizSet = QuizSet.create(quiz, title, setPart, null);
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

        if (request.isRandom()) {
            Collections.shuffle(pool);
        }
        List<Term> picked = pool.subList(0, Math.min(targetCount, pool.size()));

        // 6) 문항 생성/저장
        List<QuizQuestion> questions = new ArrayList<>(picked.size());
        int order = 0;
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

            QuizQuestion q;
            if (questionType == QuestionType.INITIALS) {
                q = QuizQuestion.textAnswer(
                        term,
                        null,
                        QuestionType.INITIALS,
                        makeQuestionText(term, questionType),
                        quizSet,
                        toKoreanInitials(koreanHead(term.getTitle()))
                );
            } else {
                q = new QuizQuestion(
                        term, null, questionType,
                        makeQuestionText(term, questionType), quizSet
                );
            }
            questions.add(q);
        }

        // 6) 문항 생성/저장
        quizQuestionRepository.saveAll(questions);

        // 6-1) 보기 생성  ← 추가
        createChoicesForQuestions(questions, pool);

        // 7) questionIds 조회
        List<Long> questionIds = em.createQuery(
                "select q.id from QuizQuestion q where q.quizSet.id = :sid order by q.id",
                Long.class
        ).setParameter("sid", quizSet.getId()).getResultList();

        // 8) 응답
        return BuiltQuizSetResponse.builder()
                .quizSetId(quizSet.getId())
                .questionIds(questionIds)
                .title(quizSet.getTitle())
                .totalQuestions(questionIds.size())
                .build();
    }

    /**
     * (세트 전용 응답) 필요한 경우 위 메서드를 호출해 DTO 변환만 수행
     */
    @Override
    @Transactional
    public CreateQuizSetByCategoryResponse registerQuizSetByCategory(CreateQuizSetByCategoryRequest request) {
        BuiltQuizSetResponse built = registerQuizSetByCategoryReturningQuestions(request);
        QuizSet set = quizSetRepository.getReferenceById(built.getQuizSetId());
        return CreateQuizSetByCategoryResponse.from(set, built.getQuestionIds());
    }

    /**
     * 즐겨찾기 기반 세션 생성
     */
    @Override
    @Transactional
    public CreateQuizSessionResponse registerQuizSetByFavorites(CreateQuizSessionRequest request) {
        Long accountId = request.getAccountId();
        Long folderId = request.getFolderId();

        // 1) 소유권 검증
        if (folderId != null) {
            boolean owned = wordbookFolderQueryService.existsByIdAndAccountId(folderId, accountId);
            if (!owned) {
                throw new SecurityException("폴더가 없거나 권한이 없습니다.");
            }
        }

        // 2) 용어 조회
        List<Term> terms = (folderId == null)
                ? wordbookTermRepository.findTermsByAccount(accountId)
                : wordbookTermRepository.findTermsByAccountAndFolderStrict(accountId, folderId);

        if (terms.isEmpty()) {
            throw new IllegalArgumentException("즐겨찾기 용어가 없습니다.");
        }

        // 3) 최근 N일 내 본 Term 회피
        int N = (request.getAvoidRecentDays() == null || request.getAvoidRecentDays() <= 0) ? 30 : request.getAvoidRecentDays();
        List<Term> filteredTerms = terms;
        try {
            if (recentUsageService != null) {
                // 서비스가 있으면 서비스 사용
                Set<Long> recentTermIds = recentUsageService.findRecentTermIds(accountId, N);
                filteredTerms = terms.stream()
                        .filter(t -> !recentTermIds.contains(t.getId()))
                        .collect(Collectors.toList());
                if (filteredTerms.isEmpty()) {
                    log.info("최근 {}일 내 본 용어로 인해 후보가 비었습니다. 회피 미적용으로 대체합니다.", N);
                    filteredTerms = terms;
                }
            } else if (sessionAnswerRepository != null) {
                // 없으면 세션 답변 리포지토리로 폴백
                LocalDateTime since = LocalDateTime.now().minusDays(N);
                List<Long> recentIds = sessionAnswerRepository.findRecentTermIdsByAccountSince(accountId, since);
                Set<Long> recentSet = Set.copyOf(recentIds);
                filteredTerms = terms.stream()
                        .filter(t -> !recentSet.contains(t.getId()))
                        .collect(Collectors.toList());
                if (filteredTerms.isEmpty()) {
                    log.info("최근 {}일 내 본 용어로 인해 후보가 비었습니다(폴백). 회피 미적용으로 대체합니다.", N);
                    filteredTerms = terms;
                }
            } else {
                log.debug("[recent-exclude] RecentUsageService/SessionAnswerRepository 미주입 → 최근 제외 스킵");
            }
        } catch (Exception e) {
            log.warn("[recent-exclude] 최근 제외 처리 중 오류 → 회피 미적용으로 진행: {}", e.toString());
            filteredTerms = terms;
        }

        // 4) 시드/난이도 설정
        SeedMode seedMode = request.getSeedMode() == null ? SeedMode.AUTO : request.getSeedMode();
        String difficulty = (request.getDifficulty() == null) ? "MEDIUM" : request.getDifficulty();
        Long fixedSeed = request.getFixedSeed();

        // 5) 문항 생성 (필터링된 풀 사용)
        List<QuizQuestion> questions = autoQuizGenerator.generateQuestions(
                filteredTerms,
                request.getQuestionTypes(),
                request.getCount(),
                request.getMcqEach(),
                request.getOxEach(),
                request.getInitialsEach(),
                seedMode,
                request.getAccountId(),     // DAILY 재현성
                fixedSeed,                  // FIXED 재현성 (nullable)
                difficulty
        );

        if (questions.isEmpty()) throw new IllegalStateException("생성된 문제가 없습니다.");

        // 6) 세트 저장 + 문항 저장(Managed 상태로)
        String title = "[GEN] Favorites";


        // 요청된 questionTypes 기반으로 세트 타입 추론
        QuizSetType setPart;
        var types = request.getQuestionTypes();
        if (types == null || types.isEmpty()) {
            setPart = QuizSetType.CHOICE;
        } else if (types.size() == 1) {
            switch (types.get(0)) {
                case CHOICE     -> setPart = QuizSetType.CHOICE;
                case OX         -> setPart = QuizSetType.OX;
                case INITIALS   -> setPart = QuizSetType.INITIALS;
                default         -> setPart = QuizSetType.CHOICE;
            }
        } else {
            setPart = QuizSetType.CHOICE;
        }

        Quiz quiz = Quiz.create(title, setPart);
        quiz = quizRepository.save(quiz);

        QuizSet quizSet = QuizSet.create(quiz, title, setPart, null);
        quizSet = quizSetRepository.save(quizSet);

        // 문항에 세트 연결
        for (QuizQuestion q : questions) {
            q.setQuizSet(quizSet);
        }
        quizQuestionRepository.saveAll(questions);

        // 7) 보기 생성·저장 (옵션 재사용 회피는 RecentUsageService 있을 때만 적용)
        if (recentUsageService != null) {
            Set<String> recentOptionNorms = recentUsageService.findRecentChoiceNorms(accountId, N);
            autoQuizGenerator.createAndSaveChoicesForWithExclusion(
                    questions, seedMode, accountId, fixedSeed, recentOptionNorms
            );
        } else {
            autoQuizGenerator.createAndSaveChoicesFor(
                    questions, seedMode, accountId, fixedSeed
            );
        }

        return CreateQuizSessionResponse.of(
                quizSet.getId(),
                questions.stream().map(QuizQuestion::getId).toList()
        );
    }

    /**
     * 카테고리에서 문항 수 만큼 용어를 선별.
     * - isRandom=true: DB 랜덤(함수 이름은 DB에 맞게 RAND()/RANDOM() 등) 정렬
     * - isRandom=false: 최근 등록순(id DESC) 예시
     */
    private List<Term> pickTermsByCategoryPolicy(Long categoryId, int count, boolean isRandom) {
        String base = "select t from Term t ";
        String where = (categoryId != null) ? "where t.category.id = :cid " : "";
        String order = isRandom
                ? "order by function('rand')"     // MySQL: rand, Postgres: random
                : "order by t.id desc";

        var q = em.createQuery(base + where + order, Term.class);
        if (categoryId != null) q.setParameter("cid", categoryId);
        q.setMaxResults(Math.max(1, Math.min(100, count)));
        return q.getResultList();
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

    /** 정답(있다면) 산출: 선택지 생성 시 확정하려면 null 유지 */
    private Integer makeCorrectAnswer(Term term, QuestionType qType) {
        return null;
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
    private List<Term> buildPoolForCategory(Long categoryId, List<Term> pickedTerms) {
        if (categoryId == null) return pickedTerms;
        return em.createQuery("select t from Term t where t.category.id = :cid", Term.class)
                .setParameter("cid", categoryId)
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

    /** INITIALS 정답 비교: 좌우 공백만 무시(trim) */
    private static boolean isCorrectInitialsByTrim(String expected, String userInput) {
        String e = (expected == null) ? "" : expected.trim();
        String u = (userInput == null) ? "" : userInput.trim();
        return e.equals(u);
    }

    private static boolean isCorrectInitialsIgnoreSpaces(String expected, String userInput) {
        String e = (expected == null) ? "" : expected.replaceAll("\\s+", "").trim();
        String u = (userInput == null) ? "" : userInput.replaceAll("\\s+", "").trim();
        return e.equals(u);
    }
}
