package com.cygnus.ipoten.quiz.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.repository.AccountRepository;
import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz.service.request.CreateQuizSessionRequest;
import com.cygnus.ipoten.term.entity.Category;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term.repository.CategoryRepository;
import com.cygnus.ipoten.term.repository.TermRepository;
import com.cygnus.ipoten.user_term.entity.UserWordbookFolder;
import com.cygnus.ipoten.user_term.entity.UserWordbookTerm;
import com.cygnus.ipoten.user_term.repository.UserWordbookFolderRepository;
import com.cygnus.ipoten.user_term.repository.UserWordbookTermRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class QuizServiceValidationIT {

    @Autowired QuizQuestionRepository quizQuestionRepository;
    @Autowired QuizSetService quizSetService;
    @Autowired AccountRepository accountRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired UserWordbookFolderRepository userWordbookFolderRepository;
    @Autowired UserWordbookTermRepository userWordbookTermRepository;
    @Autowired TermRepository termRepository;

    // 테스트용 임의 계정 ID 조회
    private long anyAccountId() {
        return accountRepository.findAll().stream()
                .findFirst().orElseThrow().getId();
    }

    // questionTypes가 null이거나 빈 리스트면 예외가 발생하는지 테스트
    @Test
    void questionTypes_null_or_empty_shouldFail() {
        long acc = anyAccountId();

        // null
        var req1 = CreateQuizSessionRequest.builder()
                .accountId(acc)
                .questionTypes(null)
                .count(5)
                .seedMode(SeedMode.FIXED)
                .difficulty("MEDIUM")
                .build();
        assertThrows(IllegalArgumentException.class, () -> quizSetService.registerQuizSetByFavorites(req1));

        // empty
        var req2 = CreateQuizSessionRequest.builder()
                .accountId(acc)
                .questionTypes(java.util.List.of())
                .count(5)
                .seedMode(SeedMode.FIXED)
                .difficulty("MEDIUM")
                .build();
        assertThrows(IllegalArgumentException.class, () -> quizSetService.registerQuizSetByFavorites(req2));
    }

    // 문제 개수(count)가 0이하이면 예외가 발생하는지 테스트
    @Test
    void count_invalid_shouldFailOrClamp() {
        long acc = anyAccountId();

        var reqZero = CreateQuizSessionRequest.builder()
                .accountId(acc)
                .questionTypes(java.util.List.of(com.cygnus.ipoten.quiz.entity.enums.QuestionType.CHOICE))
                .count(0)
                .seedMode(SeedMode.FIXED)
                .difficulty("MEDIUM")
                .build();
        assertThrows(IllegalArgumentException.class, () -> quizSetService.registerQuizSetByFavorites(reqZero));

        var reqNegative = CreateQuizSessionRequest.builder()
                .accountId(acc)
                .questionTypes(java.util.List.of(com.cygnus.ipoten.quiz.entity.enums.QuestionType.CHOICE))
                .count(-3)
                .seedMode(SeedMode.FIXED)
                .difficulty("MEDIUM")
                .build();
        assertThrows(IllegalArgumentException.class, () -> quizSetService.registerQuizSetByFavorites(reqNegative));
    }

    // 알 수 없는 난이도 입력 시 기본값(MEDIUM)으로 동작하는지 테스트
    @Test
    void difficulty_unknown_defaultsToMedium() {
        long accId = anyAccountId();
        ensureOneFavoriteFor(accId); // 즐겨찾기 최소 1개 보장

        var req = CreateQuizSessionRequest.builder()
                .accountId(accId)
                .questionTypes(List.of(QuestionType.INITIALS))
                .count(1)
                .seedMode(SeedMode.DAILY)
                .difficulty("WTF_IS_THIS")
                .build();

        var set = quizSetService.registerQuizSetByFavorites(req);
        Long firstQid = set.getQuestionIds().get(0);
        var q = quizQuestionRepository.findById(firstQid).orElseThrow();

        String stem = q.getQuestionText();
        assertTrue(stem.contains("글자수") || stem.contains("글자 수"));
        assertTrue(stem.contains("초성"));
    }

    // --- helpers -------------------------------------------------------------

    // 테스트용 계정에 최소 1개의 즐겨찾기 용어를 생성하는 헬퍼 메서드
    private void ensureOneFavoriteFor(long accountId) {
        Account acc = accountRepository.findById(accountId).orElseThrow();

        // 카테고리 생성
        Category cat = Category.builder()
                .type("GENERAL")
                .groupName("DEFAULT_GROUP")
                .name("TEST-" + System.nanoTime())
                .depth(1)
                .sortOrder(0)
                .parent(null)
                .build();
        cat = categoryRepository.save(cat);

        // 폴더 생성 (필드 접근자가 없으면 리플렉션)
        UserWordbookFolder folder = new UserWordbookFolder();
        try {
            var fAcc = folder.getClass().getDeclaredField("account");
            fAcc.setAccessible(true);
            fAcc.set(folder, acc);
            var fName = folder.getClass().getDeclaredField("folderName");
            fName.setAccessible(true);
            fName.set(folder, "VAL-TEST-" + accountId + "-" + System.nanoTime());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        folder = userWordbookFolderRepository.save(folder);

        // 용어 + 즐겨찾기
        Term term = new Term();
        term.setTitle("미디엄기본검증");
        term.setDescription("설명");
        term.setCategory(cat);
        term = termRepository.save(term);

        userWordbookTermRepository.save(new UserWordbookTerm(acc, folder, term));
    }
}
