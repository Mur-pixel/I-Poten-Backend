//package com.cygnus.ipoten.quiz.service;
//
//import com.cygnus.ipoten.quiz.entity.QuizQuestion;
//import com.cygnus.ipoten.quiz.entity.QuizSet;
//import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
//import com.cygnus.ipoten.quiz.repository.*;
//import com.cygnus.ipoten.quiz.service.generator.AutoQuizGenerator;
//import com.cygnus.ipoten.quiz.service.request.CreateQuizSessionRequest;
//import com.cygnus.ipoten.quiz.service.response.CreateQuizSessionResponse;
//import com.cygnus.ipoten.term.entity.Category;
//import com.cygnus.ipoten.term.entity.Term;
//import com.cygnus.ipoten.term.repository.CategoryRepository;
//import com.cygnus.ipoten.user_term.repository.UserWordbookTermRepository;
//import com.cygnus.ipoten.user_term.service.UserWordbookFolderQueryService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.lang.reflect.Field;
//import java.time.LocalDateTime;
//import java.util.*;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
//class QuizSetServiceRecentExclusionTest {
//
//    @Mock
//    CategoryRepository categoryRepository;
//    @Mock QuizSetRepository quizSetRepository;
//    @Mock QuizQuestionRepository quizQuestionRepository;
//    @Mock UserWordbookTermRepository userWordbookTermRepository;
//    @Mock AutoQuizGenerator autoQuizGenerator;
//    @Mock UserWordbookFolderQueryService userWordbookFolderQueryService;
//    @Mock SessionAnswerRepository sessionAnswerRepository;
//    @Mock QuizChoiceRepository quizChoiceRepository;
//
//    @InjectMocks QuizSetServiceImpl sut; // System Under Test
//
//    @BeforeEach
//    void wireExtra() throws Exception {
//        // QuizSetServiceImpl이 생성자에 sessionAnswerRepository가 없다면, 필드로 추가했을 때를 가정하여 주입
//        try {
//            Field f = QuizSetServiceImpl.class.getDeclaredField("sessionAnswerRepository");
//            f.setAccessible(true);
//            f.set(sut, sessionAnswerRepository);
//        } catch (NoSuchFieldException ignore) {
//            // 서비스에 최근 제외 로직이 아직 없다면 테스트가 실패할 수 있음(의도된 상태: TDD)
//        }
//    }
//
//    @Test
//    void excludes_recent_terms_when_enough_pool_exists() throws Exception {
//        long accountId = 10L;
//        // terms: 1..20은 "최근", 21..60은 "비최근"
//        List<Term> all = new ArrayList<>();
//        for (long i = 1; i <= 60; i++) {
//            all.add(term(i, "T"+i, "D"+i));
//        }
//
//        when(userWordbookTermRepository.findTermsByAccount(accountId))
//                .thenReturn(all);
//
//        // 최근 termIds: 1..20
//        List<Long> recent = new ArrayList<>();
//        for (long i = 1; i <= 20; i++) recent.add(i);
//        when(sessionAnswerRepository.findRecentTermIdsByAccountSince(eq(accountId), any(LocalDateTime.class)))
//                .thenReturn(recent);
//
//        // generator가 받는 terms를 캡쳐
//        ArgumentCaptor<List<Term>> termsCap = ArgumentCaptor.forClass(List.class);
//
//        // generator는 그대로 질문을 돌려줬다고 가정
//        when(autoQuizGenerator.generateQuestions(termsCap.capture(),
//                anyList(), any(), any(), any(), any(),
//                any(), any(), any(), any()))
//                .thenAnswer(inv -> {
//                    List<Term> in = inv.getArgument(0);
//                    List<QuizQuestion> qs = new ArrayList<>();
//                    for (int i = 0; i < Math.min(10, in.size()); i++) {
//                        qs.add(q(in.get(i)));
//                    }
//                    return qs;
//                });
//
//        // set 저장 시 id 부여
//        when(quizSetRepository.save(any(QuizSet.class)))
//                .thenAnswer(inv -> {
//                    QuizSet s = inv.getArgument(0);
//                    setId(s, 99L);
//                    return s;
//                });
//
//        doNothing().when(autoQuizGenerator).createAndSaveChoicesFor(anyList(), any(), any(), any());
//
//        // request 목업
//        CreateQuizSessionRequest req = mock(CreateQuizSessionRequest.class);
//        when(req.getAccountId()).thenReturn(accountId);
//        when(req.getFolderId()).thenReturn(null);
//        when(req.getQuestionTypes()).thenReturn(List.of(QuestionType.CHOICE));
//        when(req.getCount()).thenReturn(10);
//        when(req.getMcqEach()).thenReturn(null);
//        when(req.getOxEach()).thenReturn(null);
//        when(req.getInitialsEach()).thenReturn(null);
//        when(req.getSeedMode()).thenReturn(null);
//        when(req.getFixedSeed()).thenReturn(null);
//        when(req.getDifficulty()).thenReturn("MEDIUM");
//
//        CreateQuizSessionResponse res = sut.registerQuizSetByFavorites(req);
//
//        // generator로 전달된 terms 확인: 최근(1..20)은 포함되면 안 됨
//        List<Term> usedPool = termsCap.getValue();
//        assertThat(usedPool).isNotNull();
//        assertThat(usedPool.stream().map(Term::getId))
//                .doesNotContainAnyElementsOf(recent);
//
//        assertThat(res.getQuizSetId()).isEqualTo(99L);
//    }
//
//    // ===== helpers =====
//
//    private static Term term(Long id, String title, String desc) throws Exception {
//        Term t = new Term();
//        setId(t, id);
//        Field ft = Term.class.getDeclaredField("title");
//        ft.setAccessible(true);
//        ft.set(t, title);
//        Field fd = Term.class.getDeclaredField("description");
//        fd.setAccessible(true);
//        fd.set(t, desc);
//        Field fc = Term.class.getDeclaredField("category");
//        fc.setAccessible(true);
//        fc.set(t, new Category()); // dummy
//        return t;
//    }
//
//    private static QuizQuestion q(Term t) throws Exception {
//        QuizQuestion q = new QuizQuestion(t, t.getCategory(), QuestionType.CHOICE, "stem", 1);
//        setId(q, t.getId()); // 편의상 termId 재활용
//        return q;
//    }
//
//    private static void setId(Object entity, Long id) throws Exception {
//        Field f = null;
//        Class<?> c = entity.getClass();
//        // 상위 엔티티에 id가 있을 수 있어 반영
//        while (c != null) {
//            try { f = c.getDeclaredField("id"); break; }
//            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
//        }
//        if (f == null) throw new IllegalStateException("id 필드를 찾을 수 없음: " + entity.getClass());
//        f.setAccessible(true);
//        f.set(entity, id);
//    }
//}
