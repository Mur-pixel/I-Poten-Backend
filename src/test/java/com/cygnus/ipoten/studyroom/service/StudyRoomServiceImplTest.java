//package com.wowraid.jobspoon.studyroom.service;
//
//import com.wowraid.jobspoon.account.entity.Account;
//import com.wowraid.jobspoon.account.repository.AccountRepository;
//import com.wowraid.jobspoon.accountProfile.entity.AccountProfile;
//import com.wowraid.jobspoon.accountProfile.repository.AccountProfileRepository;
//import com.wowraid.jobspoon.studyroom.controller.request_Form.CreateStudyRoomRequestForm;
//import com.wowraid.jobspoon.studyroom.entity.StudyLevel;
//import com.wowraid.jobspoon.studyroom.entity.StudyLocation;
//import com.wowraid.jobspoon.studyroom.entity.StudyMember;
//import com.wowraid.jobspoon.studyroom.entity.StudyRoom;
//import com.wowraid.jobspoon.studyroom.repository.StudyMemberRepository;
//import com.wowraid.jobspoon.studyroom.repository.StudyRoomRepository;
//import com.wowraid.jobspoon.studyroom.service.request.CreateStudyRoomRequest;
//import com.wowraid.jobspoon.studyroom.service.request.ListStudyRoomRequest;
//import com.wowraid.jobspoon.studyroom.service.request.UpdateStudyRoomRequest;
//import com.wowraid.jobspoon.studyroom.service.response.CreateStudyRoomResponse;
//import com.wowraid.jobspoon.studyroom.service.response.ListStudyRoomResponse;
//import com.wowraid.jobspoon.studyroom.service.response.UpdateStudyRoomResponse;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Slice;
//import org.springframework.data.domain.SliceImpl;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//class StudyRoomServiceImplTest {
//
//    @MockBean
//    private StudyRoomRepository studyRoomRepository;
//
//    @MockBean
//    private AccountProfileRepository  accountProfileRepository;
//
//    @MockBean
//    private StudyMemberRepository  studyMemberRepository;
//
//    @Autowired
//    private StudyRoomService studyRoomService;
//
//    @Test
//    @DisplayName("스터디룸 생성 서비스 테스트")
//    void createStudyRoom() {
//        // given
//        final Long hostId = 1L;
//        CreateStudyRoomRequest request = new CreateStudyRoomRequest(
//                hostId, "테스트 제목", "테스트 설명", 4, StudyLocation.ONLINE,
//                StudyLevel.NEWBIE, List.of("백엔드"), List.of("Java")
//        );
//
//        AccountProfile fakeHost = new AccountProfile();
//        ReflectionTestUtils.setField(fakeHost, "id", hostId);
//
//        // Repository 동작 정의 (Mocking)
//        when(accountProfileRepository.findById(hostId)).thenReturn(Optional.of(fakeHost));
//        when(studyRoomRepository.save(any(StudyRoom.class))).thenAnswer(invocation -> {
//            StudyRoom studyRoomToSave = invocation.getArgument(0);
//            ReflectionTestUtils.setField(studyRoomToSave, "id", 1L);
//            return studyRoomToSave;
//        });
//
//        // when
//        CreateStudyRoomResponse response = studyRoomService.createStudyRoom(request);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getId()).isEqualTo(1L);
//        assertThat(response.getTitle()).isEqualTo("테스트 제목");
//
//        // StudyRoom과 StudyMember가 각각 한 번씩 저장되었는지 검증
//        verify(studyRoomRepository).save(any(StudyRoom.class));
//        verify(studyMemberRepository).save(any(StudyMember.class));
//    }
//
//    @Test
//    @DisplayName("스터디룸 목록 조회 서비스 테스트 (첫 페이지)")
//    void findAllStudyRooms_firstPage() {
//        // given
//        final ListStudyRoomRequest request = new ListStudyRoomRequest(null, 10);
//        final Slice<StudyRoom> fakeResult = new SliceImpl<>(Collections.emptyList());
//
//        // 👇 any()를 사용하여 어떤 PageRequest가 오든 fakeResult를 반환하도록 수정
//        when(studyRoomRepository.findAllByOrderByIdDesc(any(PageRequest.class)))
//                .thenReturn(fakeResult);
//
//        // when
//        ListStudyRoomResponse response = studyRoomService.findAllStudyRooms(request);
//
//        // then
//        assertThat(response).isNotNull(); // response가 null이 아닌지 확인
//        assertThat(response.isHasNext()).isFalse();
//    }
//
//    @Test
//    @DisplayName("스터디룸 수정 서비스 테스트 - 성공 (모임장인 경우)")
//    void updateStudyRoom_success() {
//        // given
//        final Long studyRoomId = 1L;
//        final Long currentUserId = 1L; // 현재 사용자가 스터디장
//        AccountProfile fakeHost = new AccountProfile();
//        ReflectionTestUtils.setField(fakeHost, "id", currentUserId);
//
//        UpdateStudyRoomRequest request = new UpdateStudyRoomRequest(
//                "수정된 제목", "수정된 설명", 10, StudyLocation.BUSAN,
//                StudyLevel.SENIOR, List.of("풀스택"), List.of("Kotlin")
//        );
//
//        // DB에서 찾아올 원본 스터디룸 객체
//        StudyRoom originalStudyRoom = StudyRoom.create(
//                fakeHost, "원본 제목", "원본 설명", 5, StudyLocation.SEOUL, StudyLevel.JUNIOR,
//                List.of("백엔드"), List.of("Java")
//        );
//
//        // 실제 Service 코드에서 사용하는 findByIdWithHost 메소드를 Mocking
//        when(studyRoomRepository.findByIdWithHost(studyRoomId)).thenReturn(Optional.of(originalStudyRoom));
//
//        // when
//        // 실제 Service 메소드 시그니처에 맞게 호출
//        UpdateStudyRoomResponse response = studyRoomService.updateStudyRoom(studyRoomId, currentUserId, request);
//
//        // then
//        assertThat(response.getTitle()).isEqualTo("수정된 제목");
//        assertThat(response.getDescription()).isEqualTo("수정된 설명");
//        assertThat(response.getLocation()).isEqualTo("BUSAN");
//    }
//
//    @Test
//    @DisplayName("스터디룸 수정 서비스 테스트 - 실패 (모임장이 아닌 경우)")
//    void updateStudyRoom_fail_unauthorized() {
//        // given
//        final Long studyRoomId = 1L;
//        final Long hostId = 1L; // 실제 스터디장 ID
//        final Long otherUserId = 2L; // 수정을 시도하는 다른 사용자 ID
//        AccountProfile fakeHost = new AccountProfile();
//        ReflectionTestUtils.setField(fakeHost, "id", hostId);
//
//        UpdateStudyRoomRequest request = new UpdateStudyRoomRequest(
//                "수정된 제목", "수정된 설명", 10, StudyLocation.BUSAN,
//                StudyLevel.SENIOR, List.of("풀스택"), List.of("Kotlin")
//        );
//        StudyRoom originalStudyRoom = StudyRoom.create(
//                fakeHost, "원본 제목", "원본 설명", 5, StudyLocation.SEOUL, StudyLevel.JUNIOR,
//                List.of("백엔드"), List.of("Java")
//        );
//        when(studyRoomRepository.findByIdWithHost(studyRoomId)).thenReturn(Optional.of(originalStudyRoom));
//
//        // when & then
//        // 다른 사용자가 수정을 시도하면 IllegalStateException 예외가 발생하는지 검증
//        assertThatThrownBy(() -> studyRoomService.updateStudyRoom(studyRoomId, otherUserId, request))
//                .isInstanceOf(IllegalStateException.class)
//                .hasMessage("수정 권한이 없는 사용자입니다.");
//    }
//
//    @Test
//    @DisplayName("스터디룸 삭제 서비스 테스트 - 성공 (스터디장인 경우)")
//    void deleteStudyRoom_success() {
//        // given
//        final Long studyRoomId = 1L;
//        final Long currentUserId = 1L;
//        AccountProfile fakeHost = new AccountProfile();
//        ReflectionTestUtils.setField(fakeHost, "id", currentUserId);
//
//        StudyRoom fakeStudyRoom = StudyRoom.create(
//                fakeHost, "삭제될 스터디", "설명", 5, StudyLocation.ONLINE, StudyLevel.ALL, null, null
//        );
//
//        when(studyRoomRepository.findByIdWithHost(studyRoomId)).thenReturn(Optional.of(fakeStudyRoom));
//
//        // when
//        studyRoomService.deleteStudyRoom(studyRoomId, currentUserId);
//
//        // then
//        // delete 메소드가 정확히 1번 호출되었는지 검증
//        verify(studyRoomRepository, times(1)).delete(fakeStudyRoom);
//    }
//
//    @Test
//    @DisplayName("스터디룸 삭제 서비스 테스트 - 실패 (스터디장이 아닌 경우)")
//    void deleteStudyRoom_fail_unauthorized() {
//        // given
//        final Long studyRoomId = 1L;
//        final Long hostId = 1L;
//        final Long otherUserId = 2L;
//        AccountProfile fakeHost = new AccountProfile();
//        ReflectionTestUtils.setField(fakeHost, "id", hostId);
//
//        StudyRoom fakeStudyRoom = StudyRoom.create(
//                fakeHost, "삭제될 스터디", "설명", 5, StudyLocation.ONLINE, StudyLevel.ALL, null, null
//        );
//
//        when(studyRoomRepository.findByIdWithHost(studyRoomId)).thenReturn(Optional.of(fakeStudyRoom));
//
//        // when & then
//        assertThatThrownBy(() -> studyRoomService.deleteStudyRoom(studyRoomId, otherUserId))
//                .isInstanceOf(IllegalStateException.class)
//                .hasMessage("삭제 권한이 없는 사용자입니다.");
//    }
//}