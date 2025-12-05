package com.cygnus.ipoten.quiz.service;

import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz.service.generator.AutoQuizGenerator;
import com.cygnus.ipoten.quiz.service.generator.DifficultyProperties;
import com.cygnus.ipoten.quiz.service.generator.DifficultyProperties.Profile;
import com.cygnus.ipoten.term.entity.Term;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuizGeneratorBehaviorTest {

    // 테스트용 가짜 용어(Term) 목록 생성
    private static List<Term> fakeTerms(int n, String prefix) {
        List<Term> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Term t = new Term();
            t.setTitle(prefix + i);
            t.setDescription(prefix + i + " 설명");
            list.add(t);
        }
        return list;
    }

    // 퀴즈 문제의 지문(questionText) 목록 추출
    private static List<String> stems(List<QuizQuestion> qs) {
        return qs.stream().map(QuizQuestion::getQuestionText).toList();
    }

    // --- 난이도별(INITIALS) 힌트 검증 ----------------------------------------

    // INITIALS 문제에서 난이도(EASY/MEDIUM/HARD)에 따라 힌트(글자수, 초성)가 다르게 제공되는지 테스트
    @Test
    void initialsHints_changeByDifficulty() {
        var repo = mock(QuizChoiceRepository.class);
        var props = new DifficultyProperties(); // 기본값: easy/medium/hard 프로파일 채워짐
        var gen = new AutoQuizGenerator(repo, props);

        Term term = new Term();
        term.setTitle("초성테스트");
        term.setDescription("설명");

        // EASY: 글자수 공개 + 초성 일부 공개(기본 2자 혹은 1자)
        {
            String diff = "EASY";
            List<QuizQuestion> qs = gen.generateQuestions(
                    List.of(term), List.of(QuestionType.INITIALS),
                    1, null, null, null,
                    SeedMode.FIXED, null, 123L, diff
            );
            String stem = qs.get(0).getQuestionText();
            assertTrue(stem.contains("글자수") || stem.contains("글자 수"));
            assertTrue(stem.contains("초성"));
        }

        // MEDIUM: 글자수 공개 + 초성 소량 공개
        {
            String diff = "MEDIUM";
            List<QuizQuestion> qs = gen.generateQuestions(
                    List.of(term), List.of(QuestionType.INITIALS),
                    1, null, null, null,
                    SeedMode.FIXED, null, 123L, diff
            );
            String stem = qs.get(0).getQuestionText();
            assertTrue(stem.contains("글자수") || stem.contains("글자 수"));
            assertTrue(stem.contains("초성"));
        }

        // HARD: 글자수/초성 힌트 숨김(기본값 기준)
        {
            String diff = "HARD";
            // hard 프로파일 확인(테스트 강건성)
            Profile hard = props.getProfileOrDefault("HARD");
            assertFalse(hard.isRevealLength());
            assertEquals(0, hard.getRevealInitialsCount());

            List<QuizQuestion> qs = gen.generateQuestions(
                    List.of(term), List.of(QuestionType.INITIALS),
                    1, null, null, null,
                    SeedMode.FIXED, null, 123L, diff
            );
            String stem = qs.get(0).getQuestionText();
            assertFalse(stem.contains("글자수") || stem.contains("글자 수"));
            assertFalse(stem.contains("초성"));
        }
    }

    // --- 시드 결정성 ---------------------------------------------------------
    // FIXED 시드 모드: 동일한 파라미터로 생성하면 문제 순서와 보기가 완전히 동일한지 테스트
    @Test
    void seed_FIXED_sameParams_produceIdenticalOrderAndChoices() {
        var repo = mock(QuizChoiceRepository.class);
        var props = new DifficultyProperties();
        props.getMedium().setOptionCount(4);

        var gen1 = new AutoQuizGenerator(repo, props);
        var gen2 = new AutoQuizGenerator(repo, props);

        var terms = fakeTerms(6, "T-");
        var types = List.of(QuestionType.CHOICE); // 하나만
        var a = gen1.generateQuestions(terms, types, 1, null, null, null,
                SeedMode.FIXED, 42L, 777L, "MEDIUM");
        var b = gen2.generateQuestions(terms, types, 1, null, null, null,
                SeedMode.FIXED, 42L, 777L, "MEDIUM");

        assertEquals(stems(a), stems(b));

        ArgumentCaptor<QuizChoice> capA = ArgumentCaptor.forClass(QuizChoice.class);
        gen1.createAndSaveChoicesFor(a, SeedMode.FIXED, 42L, 777L);
        verify(repo, times(props.getMedium().getOptionCount())).save(capA.capture());
        clearInvocations(repo);

        ArgumentCaptor<QuizChoice> capB = ArgumentCaptor.forClass(QuizChoice.class);
        gen2.createAndSaveChoicesFor(b, SeedMode.FIXED, 42L, 777L);
        verify(repo, times(props.getMedium().getOptionCount())).save(capB.capture());

        List<String> aTexts = capA.getAllValues().stream()
                .map(c -> c.getChoiceText() + "|" + c.isAnswer()).toList();
        List<String> bTexts = capB.getAllValues().stream()
                .map(c -> c.getChoiceText() + "|" + c.isAnswer()).toList();
        assertEquals(aTexts, bTexts);
    }

    // DAILY 시드 모드: 같은 계정·같은 날이면 동일한 순서, 다른 계정이면 다른 순서가 나오는지 테스트
    @Test
    void seed_DAILY_sameAccountSameDay_sameOrder_butDifferentAccounts_different() {
        var repo = mock(QuizChoiceRepository.class);
        var gen = new AutoQuizGenerator(repo, new DifficultyProperties());

        var terms = fakeTerms(8, "D-");
        var types = List.of(QuestionType.CHOICE);

        var sameA = gen.generateQuestions(terms, types, 5, null, null, null,
                SeedMode.DAILY, 1001L, null, "MEDIUM");
        var sameB = gen.generateQuestions(terms, types, 5, null, null, null,
                SeedMode.DAILY, 1001L, null, "MEDIUM");

        assertEquals(stems(sameA), stems(sameB)); // 같은 계정·같은 날 → 동일

        var diffAcc = gen.generateQuestions(terms, types, 5, null, null, null,
                SeedMode.DAILY, 2002L, null, "MEDIUM");

        assertNotEquals(stems(sameA), stems(diffAcc)); // 다른 계정 → 달라짐
    }

    // --- 보기(CHOICE) 유효성 --------------------------------------------------
    // CHOICE 문제: 보기 개수 준수, 중복 없음, 정답 1개, 오답≠정답 조건을 만족하는지 테스트
    @Test
    void choice_validity_optionCount_unique_singleCorrect_notEqualToWrong() {
        var repo = mock(QuizChoiceRepository.class);
        var props = new DifficultyProperties();
        props.getMedium().setOptionCount(5);
        var gen = new AutoQuizGenerator(repo, props);

        // 하나의 CHOICE 문제 생성
        Term t = new Term();
        t.setTitle("정답단어");
        t.setDescription("설명");
        var qs = gen.generateQuestions(
                List.of(t), List.of(QuestionType.CHOICE),
                1, null, null, null,
                SeedMode.FIXED, 77L, 314L, "MEDIUM"
        );

        // 저장되는 보기 캡쳐
        ArgumentCaptor<QuizChoice> cap = ArgumentCaptor.forClass(QuizChoice.class);
        gen.createAndSaveChoicesFor(qs, SeedMode.FIXED, 77L, 314L);
        verify(repo, times(5)).save(cap.capture()); // optionCount=5

        List<QuizChoice> choices = cap.getAllValues();
        // 개수
        assertEquals(5, choices.size(), "optionCount를 따라야 함");

        // 중복 없음
        var texts = choices.stream().map(QuizChoice::getChoiceText).toList();
        assertEquals(new HashSet<>(texts).size(), texts.size(), "중복 보기 금지");

        // 정답 1개
        long correctCount = choices.stream().filter(QuizChoice::isAnswer).count();
        assertEquals(1, correctCount, "정답은 정확히 1개");

        // 오답은 정답 텍스트와 달라야 함
        String answerText = choices.stream().filter(QuizChoice::isAnswer).findFirst().map(QuizChoice::getChoiceText).orElse("");
        choices.stream().filter(c -> !c.isAnswer())
                .forEach(c -> assertNotEquals(answerText, c.getChoiceText()));
    }

    // --- 유니코드/비한글 타이틀에서도 INITIALS 안전 동작 -----------------------
    // 비한글(영문, 숫자, 특수문자) 타이틀도 INITIALS 문제 생성 시 안전하게 처리되는지 테스트
    @Test
    void initials_handlesNonKoreanAndSymbols_gracefully() {
        var repo = mock(QuizChoiceRepository.class);
        var gen = new AutoQuizGenerator(repo, new DifficultyProperties());

        List<String> titles = List.of("HTTP/2", "C++", "AI", "12345", "abcXYZ");
        for (String title : titles) {
            Term t = new Term();
            t.setTitle(title);
            t.setDescription("desc");

            List<QuizQuestion> qs = gen.generateQuestions(
                    List.of(t), List.of(QuestionType.INITIALS),
                    1, null, null, null,
                    SeedMode.FIXED, 1L, 9L, "MEDIUM"
            );

            assertEquals(1, qs.size());
            String stem = qs.get(0).getQuestionText();
            assertNotNull(stem);
            assertTrue(stem.length() > 0, "지문이 비어있지 않아야 함: " + title);
        }
    }
}
