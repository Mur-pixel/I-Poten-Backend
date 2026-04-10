//package com.wowraid.jobspoon.interview;
//
//import com.wowraid.jobspoon.account.entity.Account;
//import com.wowraid.jobspoon.account.service.AccountService;
//import com.wowraid.jobspoon.interview.controller.request.InterviewQARequest;
//import com.wowraid.jobspoon.interview.controller.request.IntervieweeProfileRequest;
//import com.wowraid.jobspoon.interview.controller.request_form.InterviewCreateRequestForm;
//import com.wowraid.jobspoon.interview.entity.Interview;
//import com.wowraid.jobspoon.interview.entity.InterviewType;
//import com.wowraid.jobspoon.interviewee_profile.entity.TechStack;
//import com.wowraid.jobspoon.interview.repository.InterviewRepository;
//import com.wowraid.jobspoon.interview.service.InterviewServiceImpl;
//import com.wowraid.jobspoon.interview.service.response.InterviewCreateResponse;
//import com.wowraid.jobspoon.interviewQA.entity.InterviewQA;
//import com.wowraid.jobspoon.interviewQA.service.InterviewQAService;
//import com.wowraid.jobspoon.interviewee_profile.entity.IntervieweeProfile;
//import com.wowraid.jobspoon.interviewee_profile.service.IntervieweeProfileService;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//
//@ExtendWith(MockitoExtension.class)
//public class InterviewServiceTest {
//
//    @InjectMocks
//    private InterviewServiceImpl interviewService;
//    @Mock
//    private InterviewRepository interviewRepository;
//    @Mock
//    private AccountService accountService;
//    @Mock
//    private IntervieweeProfileService intervieweeProfileService;
//    @Mock
//    private InterviewQAService interviewQAService;
//
//
//
//    InterviewType interviewType = InterviewType.COMPANY;
//
//    InterviewCreateRequestForm form = InterviewCreateRequestForm.builder()
//            .company("당근마켓")
//            .major("전공자")
//            .career("3년 이하")
//            .projectExp("있음")
//            .job("Backend")
//            .projectDescription("job-spoon 프로젝트는 ai 면접....")
//            .techStacks(List.of(TechStack.BACKEND, TechStack.FULLSTACK))
//            .firstQuestion("첫 질문 예시?")
//            .firstAnswer("첫 답변 예시")
//            .build();
//
//
//
//
//    Account mockAccount = new Account(1L);
//
//    IntervieweeProfile mockProfile = new IntervieweeProfile(
//            "당근마켓", "전공자", "3년 이하", "있음", "Backend"
//            , List.of(TechStack.BACKEND, TechStack.FULLSTACK)
//    );
//
//    Interview interview = new Interview(mockAccount, mockProfile, interviewType);
//    InterviewQA mockInterviewQA = new InterviewQA(
//            interview,
//            "자기소개 해주세요",
//            "네 안녕하세요 ,,,,"
//    );
//
//
//    @Test
//    @DisplayName("새로운 인터뷰가 생성됩니다")
//    void 새로운_인터뷰_생성(){
//
//
//
//        // given
//        given(accountService.findById(any())).willReturn(Optional.of(mockAccount));
//
//        given(intervieweeProfileService.createIntervieweeProfile(any(IntervieweeProfileRequest.class)))
//                .willReturn(mockProfile);
//
//        given(interviewQAService.createInterviewQA(any(InterviewQARequest.class)))
//                .willReturn(mockInterviewQA);
//
//        given(interviewRepository.save(any())).willReturn(interview);
//
//
//        // when
//        InterviewCreateResponse interviewCreateResponse = interviewService.createInterview(form, 1L,"userToken");
//
//
//        // then
//        Assertions.assertNotNull(interviewCreateResponse);
//        Assertions.assertEquals(interviewCreateResponse.getInterviewId(), interview.getId());
//
//
//    }
//
//
//
//    @Test
//    @DisplayName("기술 면접 답변을 통한 질문 생성")
//    void 기술_면접_답변을_통한_질문_생성(){
//
//
//        // given
//        given(interviewRepository.findById(any())).willReturn(Optional.ofNullable(interview));
//
//        given(interviewQAService.createInterviewQA(any(InterviewQARequest.class))).willReturn(mockInterviewQA);
//
//
//        // when
//
//        // then
//
//
//
//    }
//
//
//
//
//
//
//}
