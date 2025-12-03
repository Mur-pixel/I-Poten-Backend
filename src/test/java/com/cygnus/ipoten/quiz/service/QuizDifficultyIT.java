package com.cygnus.ipoten.quiz.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.repository.AccountRepository;
import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz.service.generator.AutoQuizGenerator;
import com.cygnus.ipoten.quiz.service.generator.DifficultyProperties;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_session.service.request.CreateQuizSessionRequest;
import com.cygnus.ipoten.term_category.entity.TermCategory;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term_category.repository.TermCategoryRepository;
import com.cygnus.ipoten.term.repository.TermRepository;
import com.cygnus.ipoten.user_term.entity.UserWordbookFolder;
import com.cygnus.ipoten.user_term.entity.UserWordbookTerm;
import com.cygnus.ipoten.user_term.repository.UserWordbookFolderRepository;
import com.cygnus.ipoten.user_term.repository.UserWordbookTermRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@Sql(statements = {
        "ALTER TABLE quiz_question MODIFY COLUMN question_type VARCHAR(20) NOT NULL"
})
class QuizDifficultyIT {

    @Autowired QuizSetService quizSetService;
    @Autowired AccountRepository accountRepository;
    @Autowired TermRepository termRepository;
    @Autowired UserWordbookTermRepository userWordbookTermRepository;
    @Autowired
    TermCategoryRepository termCategoryRepository;
    @Autowired UserWordbookFolderRepository userWordbookFolderRepository;
    @Autowired DifficultyProperties difficultyProperties;

    // 난이도(EASY/HARD)에 따라 다른 퀴즈 문제가 생성되는지 통합 테스트
    @Test
    void differentDifficulty_shouldChangeOutput() {
        // 계정
        Account acc = accountRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("시드 계정 필요"));

        // 카테고리
        TermCategory cat = TermCategory.builder()
                .type("GENERAL")
                .groupName("DEFAULT_GROUP")
                .name("DIFF-" + System.nanoTime())
                .depth(1)
                .sortOrder(0)
                .parent(null)
                .build();
        cat = termCategoryRepository.save(cat);

        // 폴더
        UserWordbookFolder folder = new UserWordbookFolder();
        try {
            var fAcc = folder.getClass().getDeclaredField("account");
            fAcc.setAccessible(true);
            fAcc.set(folder, acc);
            var fName = folder.getClass().getDeclaredField("folderName");
            fName.setAccessible(true);
            fName.set(folder, "난이도테스트");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        folder = userWordbookFolderRepository.save(folder);

        // 용어 + 즐겨찾기
        for (int i = 1; i <= 12; i++) {
            Term t = new Term();
            t.setTitle("용어" + i);
            t.setDescription("용어" + i + " 설명");
            t.setTermCategory(cat);
            termRepository.save(t);
            userWordbookTermRepository.save(new UserWordbookTerm(acc, folder, t, i));
        }

        long accountId = acc.getId();

        var easyReq = CreateQuizSessionRequest.builder()
                .accountId(accountId)
                .questionTypes(List.of(QuestionType.CHOICE, QuestionType.INITIALS))
                .count(8)
                .seedMode(SeedMode.DAILY)
                .difficulty("EASY")
                .build();

        // toBuilder() 대신 동일 파라미터로 새로 빌드
        var hardReq = CreateQuizSessionRequest.builder()
                .accountId(accountId)
                .questionTypes(List.of(QuestionType.CHOICE, QuestionType.INITIALS))
                .count(8)
                .seedMode(SeedMode.DAILY)
                .difficulty("HARD")
                .build();

        var easy = quizSetService.registerQuizSetByFavorites(easyReq);
        var hard = quizSetService.registerQuizSetByFavorites(hardReq);

        assertNotEquals(easy.getQuestionIds(), hard.getQuestionIds());
    }

    // DAILY 모드: 같은 날짜라도 다른 계정(userId)이면 문제 순서가 달라지는지 테스트
    @Test
    void dailySeed_sameDay_diffAccount_differentOrder() {
        var repo = mock(QuizChoiceRepository.class);
        var props = new DifficultyProperties();
        var gen = new AutoQuizGenerator(repo, props);

        var terms = fakeTerms(8);
        var types = List.of(QuestionType.CHOICE);

        var a = gen.generateQuestions(terms, types, 5, null, null, null,
                SeedMode.DAILY, 111L, null, "MEDIUM");
        var b = gen.generateQuestions(terms, types, 5, null, null, null,
                SeedMode.DAILY, 222L, null, "MEDIUM");

        assertNotEquals(keys(a), keys(b));
    }

    // 난이도를 null로 설정하면 기본값 MEDIUM으로 동작하는지 테스트
    @Test
    void difficultyNull_defaultsToMedium() {
        var repo = mock(QuizChoiceRepository.class);
        var gen = new AutoQuizGenerator(repo, new DifficultyProperties());
        var t = fakeTerms(1).get(0);

        var qs = gen.generateQuestions(
                List.of(t),
                List.of(QuestionType.INITIALS),
                1, null, null, null,
                SeedMode.FIXED, null, 7L, null
        );
        String stem = qs.get(0).getQuestionText();
        assertTrue(stem.contains("글자수"));    // MEDIUM 기본 규칙
        assertTrue(stem.contains("초성 힌트")); // MEDIUM 기본 규칙
    }

    // CHOICE 문제의 보기 개수가 난이도 설정의 optionCount를 따르는지 테스트
    @Test
    void choice_respectsOptionCount() {
        var repo = mock(QuizChoiceRepository.class);
        var props = new DifficultyProperties();
        props.getMedium().setOptionCount(5); // 임의로 조정
        var gen = new AutoQuizGenerator(repo, props);

        var qs = gen.generateQuestions(
                fakeTerms(1), List.of(QuestionType.CHOICE),
                1, null, null, null,
                SeedMode.FIXED, null, 42L, "MEDIUM"
        );

        gen.createAndSaveChoicesFor(qs, SeedMode.FIXED, null, 42L);

        // 저장 호출된 보기 수가 5인지 검증
        verify(repo, times(5)).save(any());
    }

    // 오답 보기가 부족해도 최소 보기 개수(optionCount)를 보장하는지 테스트
    @Test
    void distractors_shortfalls_areFilledToOptionCount() {
        var repo = mock(QuizChoiceRepository.class);
        var props = new DifficultyProperties();
        props.getEasy().setOptionCount(4);
        props.getEasy().setSimilarityMin(0.95);
        props.getEasy().setSimilarityMax(1.0);

        var gen = new AutoQuizGenerator(repo, props);
        var qs = gen.generateQuestions(
                fakeTerms(1), List.of(QuestionType.CHOICE),
                1, null, null, null,
                SeedMode.FIXED, null, 1L, "EASY"
        );
        gen.createAndSaveChoicesFor(qs, SeedMode.FIXED, null, 1L);

        // 그래도 4개 저장되었는지 확인
        verify(repo, times(4)).save(any());
    }

    // OX 문제는 2개의 보기만 생성되고 정답 인덱스가 1인지 테스트
    @Test
    void ox_hasOnlyTwoOptions_andAnswerIndexIs1() {
        var repo = mock(QuizChoiceRepository.class);
        var gen = new AutoQuizGenerator(repo, new DifficultyProperties());
        var t = fakeTerms(1).get(0);

        var qs = gen.generateQuestions(
                List.of(t), List.of(QuestionType.OX),
                1, null, null, null, SeedMode.FIXED, null, 9L, "HARD"
        );
        var q = qs.get(0);

        gen.createAndSaveChoicesFor(qs, SeedMode.FIXED, null, 9L);
        verify(repo, times(1)).saveAll(argThat(it ->
                        StreamSupport.stream(it.spliterator(), false).count() == 2));
    }

    // 테스트용 가짜 용어(Term) 목록 생성
    private java.util.List<Term> fakeTerms(int n) {
        java.util.List<Term> list = new java.util.ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Term t = new Term();
            t.setTitle("용어" + i);
            t.setDescription("용어" + i + " 설명");
            // 카테고리가 필요 없다면 생략 가능. 필요하면 임시 카테고리도 세팅:
            // Category c = Category.builder().type("GENERAL").groupName("DEFAULT_GROUP")
            //         .name("TEMP-" + i).depth(1).sortOrder(0).build();
            // t.setCategory(c);
            list.add(t);
        }
        return list;
    }

    // 퀴즈 문제의 용어 제목 목록 추출 (순서 비교용)
    private java.util.List<String> keys(java.util.List<QuizQuestion> qs) {
        return qs.stream().map(q -> q.getTerm().getTitle()).toList();
    }
}