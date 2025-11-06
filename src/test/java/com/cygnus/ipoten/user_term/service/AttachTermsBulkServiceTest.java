package com.cygnus.ipoten.user_term.service;

import com.cygnus.ipoten.account.repository.AccountRepository;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term.repository.TermRepository;
import com.cygnus.ipoten.term.repository.TermTagRepository;
import com.cygnus.ipoten.user_term.entity.UserWordbookFolder;
import com.cygnus.ipoten.user_term.entity.UserWordbookTerm;
import com.cygnus.ipoten.user_term.repository.UserTermProgressRepository;
import com.cygnus.ipoten.user_term.repository.UserWordbookFolderRepository;
import com.cygnus.ipoten.user_term.repository.UserWordbookTermRepository;
import com.cygnus.ipoten.user_term.service.request.AttachTermsBulkRequest;
import com.cygnus.ipoten.user_term.service.response.AttachTermsBulkResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserWordbookFolderServiceImpl.attachTermsBulk 단위 테스트
 *
 * 테스트 목적:
 * - 사용자 단어장 폴더에 용어를 일괄 추가하는 기능 검증
 * - 중복 용어 스킵, 존재하지 않는 용어 처리, 권한 검증 등 엣지 케이스 확인
 *
 * 주요 검증 사항:
 * 1. 정상 추가: 모든 용어가 새롭게 추가되는 경우
 * 2. 중복 스킵: 이미 폴더에 있는 용어는 건너뛰기
 * 3. 부분 성공: 일부 용어는 유효하지 않아도 나머지는 추가
 * 4. 예외 처리: 빈 요청, 개수 초과, 권한 없음 등
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // 불필요 스터빙 예외 완화
class AttachTermsBulkServiceTest {

    @InjectMocks
    private UserWordbookFolderServiceImpl service;

    @Mock private UserWordbookFolderRepository userWordbookFolderRepository;
    @Mock private UserWordbookTermRepository userWordbookTermRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private TermRepository termRepository;
    @Mock private UserTermProgressRepository userTermProgressRepository;
    @Mock private TermTagRepository termTagRepository;

    private final Long accountId = 10L;
    private final Long folderId = 11L;
    private UserWordbookFolder folderMock;

    @BeforeEach
    void setUp() {
        // folder.getAccount().getId() 체인 대응
        folderMock = mock(UserWordbookFolder.class, RETURNS_DEEP_STUBS);
        when(folderMock.getAccount().getId()).thenReturn(accountId);

        // 공통: 폴더 존재/소유자 매칭
        when(userWordbookFolderRepository.findById(folderId)).thenReturn(Optional.of(folderMock));

        // saveAll은 넘겨준 Iterable 그대로 반환
        when(userWordbookTermRepository.saveAll(any(Iterable.class)))
                .thenAnswer(invocation -> {
                    Iterable<UserWordbookTerm> it = invocation.getArgument(0);
                    // Iterable 그대로 반환 (List로 감싸서 반환 형태 보존)
                    List<UserWordbookTerm> list = new ArrayList<>();
                    for (UserWordbookTerm u : it) list.add(u);
                    return list;
                });
    }

    /**
     * 실제 Term 인스턴스를 만들고 id 필드를 리플렉션으로 주입
     * JPA 엔티티는 보통 id를 직접 설정할 수 없어서 리플렉션 사용
     */
    private Term termWithId(Long id) {
        Term t = new Term(); // JPA 엔티티 기본 생성자 가정
        // 필드명이 다르면 "id" 대신 실제 필드명으로 바꿔주세요.
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    /**
     * 테스트용 요청 객체 생성 헬퍼 메서드
     */
    private AttachTermsBulkRequest req(List<Long> ids) {
        return new AttachTermsBulkRequest(accountId, folderId, ids, AttachTermsBulkRequest.DedupeMode.SKIP);
    }

    /**
     * Iterable의 크기를 확인하는 헬퍼 메서드 (Mockito argThat용)
     */
    private static boolean iterableSizeIs(Iterable<?> iter, int expected) {
        int n = 0; for (Object ignored : iter) n++; return n == expected;
    }

    /**
     * 테스트 1: 모든 용어가 새로운 경우
     *
     * 시나리오:
     * - 폴더에 기존 용어 없음
     * - 요청: [1, 2, 3] 모두 유효한 용어
     *
     * 기대 결과:
     * - requested=3, attached=3, skipped=0, failed=0
     * - invalidIds는 비어있음
     * - saveAll이 3개 항목으로 호출됨
     */
    @Test
    void allNewTerms_shouldAttachAll_andSkipZero_andInvalidZero() {
        // given: 폴더엔 아무것도 없음
        when(userWordbookTermRepository.findDistinctTermIdsByFolderAndAccountOrderByTermIdAsc(folderId, accountId))
                .thenReturn(List.of());
        when(userWordbookTermRepository.findMaxSortOrderByAccountAndFolder(accountId, folderId))
                .thenReturn(0);

        List<Long> input = List.of(1L, 2L, 3L);

        // 존재하는 term: 1,2,3
        when(termRepository.findAllById(input))
                .thenReturn(input.stream().map(this::termWithId).toList());
        when(termRepository.getReferenceById(anyLong()))
                .thenAnswer(inv -> termWithId(inv.getArgument(0)));

        // when
        AttachTermsBulkResponse res = service.attachTermsBulk(req(input));

        // then
        assertEquals(3, res.requested());
        assertEquals(3, res.attached());
        assertEquals(0, res.skipped());
        assertEquals(0, res.failed());
        assertTrue(res.invalidIds().isEmpty());

        verify(userWordbookTermRepository, atLeastOnce())
                .saveAll(argThat(iter -> iterableSizeIs(iter, 3)));
    }

    /**
     * 테스트 2: 기존 용어와 새 용어가 섞인 경우
     *
     * 시나리오:
     * - 폴더에 이미 [2, 3]이 있음
     * - 요청: [1, 2, 3, 4, 5]
     *
     * 기대 결과:
     * - 중복인 [2, 3]은 스킵 (skipped=2)
     * - 새로운 [1, 4, 5]만 추가 (attached=3)
     * - requested=5, failed=0, invalidIds=[]
     */
    @Test
    void mixExistingAndNew_shouldSkipDuplicates_andAttachOnlyNew() {
        // given: 폴더에는 2,3이 이미 있음
        when(userWordbookTermRepository.findDistinctTermIdsByFolderAndAccountOrderByTermIdAsc(folderId, accountId))
                .thenReturn(List.of(2L, 3L));
        when(userWordbookTermRepository.findMaxSortOrderByAccountAndFolder(accountId, folderId))
                .thenReturn(10);

        // 요청: 1,2,3,4,5  (2,3은 중복)
        List<Long> input = List.of(1L, 2L, 3L, 4L, 5L);

        // 후보(중복 제외) = 1,4,5 → 모두 존재
        when(termRepository.findAllById(List.of(1L, 4L, 5L)))
                .thenReturn(List.of(termWithId(1L), termWithId(4L), termWithId(5L)));
        when(termRepository.getReferenceById(anyLong()))
                .thenAnswer(inv -> termWithId(inv.getArgument(0)));

        // when
        AttachTermsBulkResponse res = service.attachTermsBulk(req(input));

        // then
        assertEquals(5, res.requested());
        assertEquals(3, res.attached()); // 1,4,5
        assertEquals(2, res.skipped());  // 2,3
        assertEquals(0, res.failed());
        assertTrue(res.invalidIds().isEmpty());

        verify(userWordbookTermRepository, atLeastOnce())
                .saveAll(argThat(iter -> iterableSizeIs(iter, 3)));
    }

    /**
     * 테스트 3: 유효하지 않은 용어 ID가 포함된 경우 (부분 성공)
     *
     * 시나리오:
     * - 폴더는 비어있음
     * - 요청: [1, 9999, 2] (9999는 존재하지 않는 용어)
     *
     * 기대 결과:
     * - 유효한 [1, 2]만 추가 (attached=2)
     * - 9999는 invalidIds로 반환
     * - 전체 실패가 아닌 부분 성공 처리
     */
    @Test
    void includeInvalidTerms_shouldPartiallySucceed_andReportInvalidIds() {
        // given: 폴더 빈 상태
        when(userWordbookTermRepository.findDistinctTermIdsByFolderAndAccountOrderByTermIdAsc(folderId, accountId))
                .thenReturn(List.of());
        when(userWordbookTermRepository.findMaxSortOrderByAccountAndFolder(accountId, folderId))
                .thenReturn(0);

        // 요청: 1, 9999(없는 것), 2
        List<Long> input = List.of(1L, 9999L, 2L);

        // termRepository가 1,2만 반환 → 9999는 invalid
        when(termRepository.findAllById(input))
                .thenReturn(List.of(termWithId(1L), termWithId(2L)));
        when(termRepository.getReferenceById(anyLong()))
                .thenAnswer(inv -> termWithId(inv.getArgument(0)));

        // when
        AttachTermsBulkResponse res = service.attachTermsBulk(req(input));

        // then
        assertEquals(3, res.requested());
        assertEquals(2, res.attached()); // 1,2
        assertEquals(0, res.skipped());
        assertEquals(0, res.failed());
        assertEquals(List.of(9999L), res.invalidIds());

        verify(userWordbookTermRepository, atLeastOnce())
                .saveAll(argThat(iter -> iterableSizeIs(iter, 2)));
    }

    /**
     * 테스트 4: 빈 요청 처리
     *
     * 시나리오:
     * - termIds가 빈 배열로 요청됨
     *
     * 기대 결과:
     * - 400 Bad Request 예외 발생
     * - "용어 ID 목록이 비어있습니다" 메시지
     */
    @Test
    void emptyRequest_shouldThrowBadRequest400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.attachTermsBulk(req(List.of())));
        assertEquals(400, ex.getStatusCode().value());
        assertNotNull(ex.getReason());
    }

    /**
     * 테스트 5: 최대 개수 초과 요청
     *
     * 시나리오:
     * - 2001개 용어 ID를 한 번에 요청 (최대 2000개 제한)
     *
     * 기대 결과:
     * - 400 Bad Request 예외 발생
     * - "최대 허용 개수를 초과했습니다" 메시지
     */
    @Test
    void overLimit_shouldThrowBadRequest400() {
        // 2001개 생성
        List<Long> big = new ArrayList<>();
        for (int i = 0; i < 2001; i++) big.add((long) i + 1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.attachTermsBulk(req(big)));

        assertEquals(400, ex.getStatusCode().value());
        assertNotNull(ex.getReason());
    }

    /**
     * 테스트 6: 폴더 소유권 검증 실패
     *
     * 시나리오:
     * - 요청한 폴더가 다른 사용자(userId=999)의 것
     * - 현재 사용자는 userId=10
     *
     * 기대 결과:
     * - 403 Forbidden 예외 발생
     * - "접근 권한이 없습니다" 메시지
     */
    @Test
    void wrongOwnerFolder_shouldThrowForbidden403() {
        // given: 폴더는 있으나 소유자 불일치
        UserWordbookFolder otherFolder = mock(UserWordbookFolder.class, RETURNS_DEEP_STUBS);
        when(otherFolder.getAccount().getId()).thenReturn(999L);
        when(userWordbookFolderRepository.findById(folderId)).thenReturn(Optional.of(otherFolder));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.attachTermsBulk(req(List.of(1L))));
        assertEquals(403, ex.getStatusCode().value());
    }

    /**
     * (미구현) 테스트 7: FAIL 모드 - 중복 발견 시 전체 실패
     *
     * 계획:
     * - DedupeMode.FAIL로 요청 시
     * - 중복 용어가 하나라도 있으면 409 Conflict 예외 발생
     * - 전체 롤백 처리
     *
     * TODO: FAIL 모드 구현 후 @Disabled 제거
     */
    @Disabled("dedupeMode FAIL 미구현 — 중복 발견 시 409로 전체 롤백 구현 후 활성화")
    @Test
    void dedupeModeFail_shouldConflict409_whenAnyDuplicateExists() {
        when(userWordbookFolderRepository.findById(folderId)).thenReturn(Optional.of(folderMock));
        when(userWordbookTermRepository.findDistinctTermIdsByFolderAndAccountOrderByTermIdAsc(folderId, accountId))
                .thenReturn(List.of(2L));

        AttachTermsBulkRequest req = new AttachTermsBulkRequest(
                accountId, folderId, List.of(1L, 2L), AttachTermsBulkRequest.DedupeMode.FAIL);

        assertThrows(ResponseStatusException.class, () -> service.attachTermsBulk(req));
    }

    /**
     * (미구현) 테스트 8: UPSERT 모드 - 중복 시 업데이트
     *
     * 계획:
     * - DedupeMode.UPSERT로 요청 시
     * - 중복 용어는 sortOrder 등을 업데이트
     * - 새 용어는 신규 추가
     *
     * TODO: UPSERT 모드 구현 후 @Disabled 제거
     */
    @Disabled("dedupeMode UPSERT 미구현 — 현재는 SKIP과 동일 동작")
    @Test
    void dedupeModeUpsert_shouldBehaveLikeSkip_forNow() {
        // 구현 시 활성화
    }
}