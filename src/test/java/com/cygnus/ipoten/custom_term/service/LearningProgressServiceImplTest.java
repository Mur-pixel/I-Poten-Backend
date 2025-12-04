package com.cygnus.ipoten.custom_term.service;

import com.cygnus.ipoten.custom_term_learning.service.LearningProgressServiceImpl;
import com.cygnus.ipoten.wordbook.service.WordbookFolderQueryService;
import com.cygnus.ipoten.term.repository.TermRepository;
import com.cygnus.ipoten.custom_term_learning.entity.LearningProgress;
import com.cygnus.ipoten.custom_term_learning.entity.enums.LearningStatus;
import com.cygnus.ipoten.custom_term_learning.repository.LearningProgressRepository;
import com.cygnus.ipoten.custom_term_learning.service.request.UpdateLearningProgressRequest;
import com.cygnus.ipoten.custom_term_learning.service.response.UpdateLearningProgressResponse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearningProgressServiceImplTest {

    @Mock private LearningProgressRepository learningProgressRepository;
    @Mock private TermRepository termRepository;
    @Mock private EntityManager em;
    @Mock private WordbookFolderQueryService wordbookFolderQueryService;

    private LearningProgressServiceImpl sut; // system under test

    @BeforeEach
    void setUp() {
        sut = new LearningProgressServiceImpl(
                learningProgressRepository,
                termRepository,
                em,
                wordbookFolderQueryService
        );
        // @Transactional 내부에서 afterCommit 등록을 하기 때문에, 동기화 컨텍스트를 열어준다.
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private static UpdateLearningProgressRequest req(long accountId, long termId, LearningStatus status) {
        return new UpdateLearningProgressRequest(accountId, termId, status);
    }

    @Test
    void 상태가_변경될_때_lastStudiedAt_가_설정되고_MEMORIZED면_memorizedAt도_설정된다() {
        // given
        long accountId = 1L;
        long termId = 100L;

        when(termRepository.findById(termId)).thenReturn(Optional.of(mock(com.cygnus.ipoten.term.entity.Term.class)));
        when(em.getReference(com.cygnus.ipoten.account.entity.Account.class, accountId))
                .thenReturn(mock(com.cygnus.ipoten.account.entity.Account.class));
        when(em.getReference(com.cygnus.ipoten.term.entity.Term.class, termId))
                .thenReturn(mock(com.cygnus.ipoten.term.entity.Term.class));

        // 기존 진행상태: LEARNING
        var id = new LearningProgress.Id(accountId, termId);
        var progress = LearningProgress.newOf(
                mock(com.cygnus.ipoten.account.entity.Account.class),
                mock(com.cygnus.ipoten.term.entity.Term.class)
        );
        // repository 에 기존 row 있다고 가정
        when(learningProgressRepository.findById(id)).thenReturn(Optional.of(progress));

        // when: MEMORIZED 로 변경(토글)
        UpdateLearningProgressResponse res = sut.updateMemorization(req(accountId, termId, LearningStatus.DONE));

        // then
        assertNotNull(res.getLastStudiedAt(), "lastStudiedAt must be set on toggle");
        assertNotNull(res.getMemorizedAt(), "memorizedAt must be set when status becomes MEMORIZED");
        assertEquals(LearningStatus.DONE, res.getStatus());
        // changed 플래그가 정확히 반영되는지까지 체크하려면 아래도 활성화
        // assertTrue(res.isChanged());
    }

    @Test
    void 동일_상태로_연속_클릭해도_lastStudiedAt_는_매번_갱신된다() throws Exception {
        // given
        long accountId = 1L;
        long termId = 200L;

        when(termRepository.findById(termId)).thenReturn(Optional.of(mock(com.cygnus.ipoten.term.entity.Term.class)));
        when(em.getReference(com.cygnus.ipoten.account.entity.Account.class, accountId))
                .thenReturn(mock(com.cygnus.ipoten.account.entity.Account.class));
        when(em.getReference(com.cygnus.ipoten.term.entity.Term.class, termId))
                .thenReturn(mock(com.cygnus.ipoten.term.entity.Term.class));

        var id = new LearningProgress.Id(accountId, termId);
        var progress = LearningProgress.newOf(
                mock(com.cygnus.ipoten.account.entity.Account.class),
                mock(com.cygnus.ipoten.term.entity.Term.class)
        );
        // 초기 상태를 MEMORIZED 로 만들어 둔다.
        progress.changeStatus(LearningStatus.DONE);

        // 같은 인스턴스를 레포에서 계속 돌려주어, 내부 필드 변화가 누적되도록 한다.
        when(learningProgressRepository.findById(id)).thenReturn(Optional.of(progress));

        // when 1: 동일 상태(MEMORIZED)로 첫 클릭 → lastStudiedAt1
        UpdateLearningProgressResponse r1 = sut.updateMemorization(req(accountId, termId, LearningStatus.DONE));
        LocalDateTime t1 = r1.getLastStudiedAt();
        assertNotNull(t1, "first click: lastStudiedAt must be set");

        // 분해능 차이로 같은 tick 이 될 수 있어 아주 짧게 대기
        Thread.sleep(5);

        // when 2: 동일 상태(MEMORIZED)로 두 번째 클릭 → lastStudiedAt2
        UpdateLearningProgressResponse r2 = sut.updateMemorization(req(accountId, termId, LearningStatus.DONE));
        LocalDateTime t2 = r2.getLastStudiedAt();
        assertNotNull(t2, "second click: lastStudiedAt must be set");

        // then: 매번 갱신되어야 함
        assertTrue(t2.isAfter(t1) || !t2.equals(t1),
                "second click should update lastStudiedAt (be after or at least different)");
        assertEquals(LearningStatus.DONE, r2.getStatus());
        // 동일 상태이므로 변경 여부가 false 인지 확인하려면 아래도 활성화
        // assertFalse(r2.isChanged());
    }
}
