package com.cygnus.ipoten.studyApplication.service;

import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.repository.AccountProfileRepository;
import com.cygnus.ipoten.studyApplication.entity.StudyApplication;
import com.cygnus.ipoten.studyApplication.repository.StudyApplicationRepository;
import com.cygnus.ipoten.studyApplication.service.request.CreateStudyApplicationRequest;
import com.cygnus.ipoten.studyApplication.service.response.CreateStudyApplicationResponse;
import com.cygnus.ipoten.studyroom.entity.StudyRoom;
import com.cygnus.ipoten.studyroom.repository.StudyRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyApplicationServiceImplTest {

    @InjectMocks
    private StudyApplicationServiceImpl studyApplicationService;

    @Mock
    private StudyApplicationRepository studyApplicationRepository;
    @Mock
    private StudyRoomRepository studyRoomRepository;
    @Mock
    private AccountProfileRepository accountProfileRepository;

    @Test
    @DisplayName("스터디 지원 서비스 테스트 - 성공")
    void applyToStudy_success() {
        // given
        final Long applicantId = 1L;
        final Long hostId = 2L; // 지원자와 다른 ID
        final Long studyRoomId = 14L;

        CreateStudyApplicationRequest request = new CreateStudyApplicationRequest(studyRoomId, applicantId, "지원합니다.");

        AccountProfile fakeApplicant = createFakeProfile(applicantId);
        AccountProfile fakeHost = createFakeProfile(hostId);
        StudyRoom fakeStudyRoom = createFakeStudyRoom(studyRoomId, fakeHost);

        // Repository 동작 Mocking
        when(accountProfileRepository.findById(applicantId)).thenReturn(Optional.of(fakeApplicant));
        when(studyRoomRepository.findById(studyRoomId)).thenReturn(Optional.of(fakeStudyRoom));
        when(studyApplicationRepository.existsByStudyRoomAndApplicant(fakeStudyRoom, fakeApplicant)).thenReturn(false);
        when(studyApplicationRepository.save(any(StudyApplication.class))).thenAnswer(invocation -> {
            StudyApplication app = invocation.getArgument(0);
            ReflectionTestUtils.setField(app, "id", 1L);
            return app;
        });

        // when
        CreateStudyApplicationResponse response = studyApplicationService.applyToStudy(request);

        // then
        assertThat(response.getApplicationId()).isEqualTo(1L);
        verify(studyApplicationRepository).save(any(StudyApplication.class));
    }

    @Test
    @DisplayName("스터디 지원 서비스 테스트 - 실패 (스터디장이 지원)")
    void applyToStudy_fail_hostIsApplicant() {
        // given
        final Long applicantId = 1L;
        final Long hostId = 1L; // 지원자와 같은 ID
        final Long studyRoomId = 14L;

        CreateStudyApplicationRequest request = new CreateStudyApplicationRequest(studyRoomId, applicantId, "지원합니다.");

        AccountProfile fakeApplicant = createFakeProfile(applicantId);
        StudyRoom fakeStudyRoom = createFakeStudyRoom(studyRoomId, fakeApplicant); // host가 지원자와 동일

        when(accountProfileRepository.findById(applicantId)).thenReturn(Optional.of(fakeApplicant));
        when(studyRoomRepository.findById(studyRoomId)).thenReturn(Optional.of(fakeStudyRoom));

        // when & then
        assertThatThrownBy(() -> studyApplicationService.applyToStudy(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("모임장은 자신의 스터디에 지원할 수 없습니다.");
    }

    @Test
    @DisplayName("스터디 지원 서비스 테스트 - 실패 (중복 지원)")
    void applyToStudy_fail_alreadyApplied() {
        // given
        final Long applicantId = 1L;
        final Long hostId = 2L;
        final Long studyRoomId = 14L;

        CreateStudyApplicationRequest request = new CreateStudyApplicationRequest(studyRoomId, applicantId, "지원합니다.");

        AccountProfile fakeApplicant = createFakeProfile(applicantId);
        AccountProfile fakeHost = createFakeProfile(hostId);
        StudyRoom fakeStudyRoom = createFakeStudyRoom(studyRoomId, fakeHost);

        when(accountProfileRepository.findById(applicantId)).thenReturn(Optional.of(fakeApplicant));
        when(studyRoomRepository.findById(studyRoomId)).thenReturn(Optional.of(fakeStudyRoom));
        // 이미 지원한 상황을 Mocking
        when(studyApplicationRepository.existsByStudyRoomAndApplicant(fakeStudyRoom, fakeApplicant)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> studyApplicationService.applyToStudy(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 해당 스터디모임에 지원했습니다.");
    }

    // 테스트용 가짜 객체 생성 헬퍼 메소드
    private AccountProfile createFakeProfile(Long id) {
        AccountProfile profile = new AccountProfile();
        ReflectionTestUtils.setField(profile, "id", id);
        return profile;
    }

    private StudyRoom createFakeStudyRoom(Long id, AccountProfile host) {
        StudyRoom studyRoom = StudyRoom.create(host, "테스트 스터디", "설명", 5, null, null, null, null);
        ReflectionTestUtils.setField(studyRoom, "id", id);
        return studyRoom;
    }
}