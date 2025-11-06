package com.cygnus.ipoten.quiz.service.generator;

import com.cygnus.ipoten.quiz.entity.QuizQuestion;
import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz.repository.QuizChoiceRepository;
import com.cygnus.ipoten.term.entity.Term;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AutoQuizGeneratorTest {

    private final QuizChoiceRepository quizChoiceRepository = Mockito.mock(QuizChoiceRepository.class);
    private final DifficultyProperties difficultyProperties = new DifficultyProperties();
    private final AutoQuizGenerator generator =
            new AutoQuizGenerator(quizChoiceRepository, difficultyProperties);

    // DAILY 시드 모드: 동일한 날짜(dailySeed)로 여러 번 생성해도 같은 문제 순서가 나오는지 테스트
    @Test
    void generateQuestions_dailySeed_reproducible() {
        var terms = fakeTerms(10);
        var types = List.of(QuestionType.CHOICE);

        var r1 = generator.generateQuestions(terms, types, 5, null, null, null,
                SeedMode.DAILY, 100L, null, "MEDIUM");
        var r2 = generator.generateQuestions(terms, types, 5, null, null, null,
                SeedMode.DAILY, 100L, null, "MEDIUM");

        assertThat(r1.stream().map(QuizQuestion::getQuestionText).toList())
                .isEqualTo(r2.stream().map(QuizQuestion::getQuestionText).toList());
    }

    // FIXED 시드 모드: fixedSeed가 같으면 dailySeed가 달라도 동일한 문제 순서가 나오는지 테스트
    @Test
    void fixed_sameFixedSeed_sameOrder() {
        var gen = new AutoQuizGenerator(quizChoiceRepository, difficultyProperties);
        var terms = fakeTerms(10);
        var a = gen.generateQuestions(terms, List.of(QuestionType.CHOICE), 5, null, null, null,
                SeedMode.FIXED, 1L, 777L, "MEDIUM");
        var b = gen.generateQuestions(terms, List.of(QuestionType.CHOICE), 5, null, null, null,
                SeedMode.FIXED, 999L, 777L, "MEDIUM");
        assertEquals(ids(a), ids(b));
    }

    // AUTO 시드 모드: 매번 랜덤하게 생성되어 보통은 다른 문제 순서가 나오는지 테스트
    @Test
    void auto_usuallyDifferent() {
        var gen = new AutoQuizGenerator(quizChoiceRepository, difficultyProperties);
        var terms = fakeTerms(10);
        var a = gen.generateQuestions(terms, List.of(QuestionType.CHOICE), 5, null, null, null,
                SeedMode.AUTO, 1L, null, "MEDIUM");
        var b = gen.generateQuestions(terms, List.of(QuestionType.CHOICE), 5, null, null, null,
                SeedMode.AUTO, 1L, null, "MEDIUM");
        assertNotEquals(ids(a), ids(b));
    }

    // 퀴즈 문제의 용어 ID 목록 추출
    private List<Long> ids(List<QuizQuestion> qs){
        return qs.stream().map(q -> q.getTerm().getId()).toList();
    }

    // 테스트용 가짜 용어(Term) 목록 생성
    private List<Term> fakeTerms(int n) {
        List<Term> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Term t = new Term();
            setField(t, "id", (long) i);
            setField(t, "title", "용어" + i);
            setField(t, "description", "용어" + i + "에 대한 설명이다");
            list.add(t);
        }
        return list;
    }

    // 리플렉션을 사용하여 private 필드에 값 설정
    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
