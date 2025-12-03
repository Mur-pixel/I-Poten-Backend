package com.cygnus.ipoten.quiz.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.repository.AccountRepository;
import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz_question.repository.QuizQuestionRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class QuizSetServiceImplIT {

    @Autowired QuizSetService quizSetService;
    @Autowired AccountRepository accountRepository;
    @Autowired TermRepository termRepository;
    @Autowired UserWordbookTermRepository userWordbookTermRepository;
    @Autowired
    TermCategoryRepository termCategoryRepository;
    @Autowired UserWordbookFolderRepository userWordbookFolderRepository;
    @Autowired QuizQuestionRepository quizQuestionRepository;

    /**
     * 리플렉션으로 필드 세팅: candidates 중 존재하는 첫 필드에 value 세팅 (없으면 조용히 스킵)
     * 프로젝트별로 필드명이 다를 수 있어 여러 후보를 시도
     */
    private static void safeSetOneOf(Object target, Object value, String... candidates) {
        for (String c : candidates) {
            try {
                Field f = target.getClass().getDeclaredField(c);
                f.setAccessible(true);
                f.set(target, value);
                System.out.println("[set] " + target.getClass().getSimpleName() + "." + c);
                return;
            } catch (NoSuchFieldException ignored) {
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("[skip] none of fields exist: " + String.join(", ", candidates));
    }

    // DAILY 모드: 동일한 요청을 여러 번 실행해도 같은 용어(Term) 순서로 문제가 생성되는지 통합 테스트
    @Test
    void daily_sameRequest_sameQuestionCONTENT() {
        // 1) 계정: 존재 계정 재사용
        Account acc = accountRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("테스트용 계정이 필요합니다(시드 확인)."));

        // 2) 카테고리 ((name, depth) 유니크라 name에 nanoTime 섞음)
        TermCategory cat = TermCategory.builder()
                .type("GENERAL")
                .groupName("DEFAULT_GROUP")
                .name("TEST-" + System.nanoTime())
                .depth(1)
                .sortOrder(0)
                .parent(null)
                .build();
        cat = termCategoryRepository.save(cat);

        // 3) 폴더 (필드명이 프로젝트별로 다를 수 있어 후보군으로 세팅)
        UserWordbookFolder folder = new UserWordbookFolder();
        // account-like 필드
        safeSetOneOf(folder, acc, "account", "owner", "user", "member");
        // name-like 필드
        safeSetOneOf(folder, "기본 폴더", "name", "title", "folderName", "label");
        folder = userWordbookFolderRepository.save(folder);

        // 4) 용어 + 즐겨찾기
        for (int i = 1; i <= 15; i++) {
            Term t = new Term();
            t.setTitle("용어" + i);
            t.setDescription("용어" + i + " 설명");
            t.setTermCategory(cat);
            termRepository.save(t);

            userWordbookTermRepository.save(new UserWordbookTerm(acc, folder, t));
        }

        var req = CreateQuizSessionRequest.builder()
                .accountId(acc.getId())
                .questionTypes(List.of(QuestionType.CHOICE, QuestionType.OX))
                .count(8)
                .seedMode(SeedMode.DAILY)
                .difficulty("MEDIUM")
                .build();

        var r1 = quizSetService.registerQuizSetByFavorites(req);
        var r2 = quizSetService.registerQuizSetByFavorites(req);

        // === 핵심: question PK 대신, 각 question이 가리키는 Term ID 시퀀스를 비교 ===
        List<Long> r1TermIds = mapQuestionIdsToTermIdsInOrder(r1.getQuestionIds());
        List<Long> r2TermIds = mapQuestionIdsToTermIdsInOrder(r2.getQuestionIds());

        assertEquals(r1TermIds, r2TermIds, "DAILY 모드에서는 선정된 Term 시퀀스가 동일해야 합니다.");
    }

    // 퀴즈 문제 ID 목록을 각 문제가 참조하는 용어(Term) ID 목록으로 변환
    private List<Long> mapQuestionIdsToTermIdsInOrder(List<Long> questionIds) {
        List<Long> termIds = new ArrayList<>(questionIds.size());
        for (Long qid : questionIds) {
            var qq = quizQuestionRepository.findById(qid)
                    .orElseThrow(() -> new IllegalStateException("QuizQuestion not found: " + qid));
            // QuizQuestion.getTerm().getId() 로 매핑
            termIds.add(qq.getTerm().getId());
        }
        return termIds;
    }
}
