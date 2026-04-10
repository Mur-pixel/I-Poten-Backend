package com.cygnus.ipoten.interview.service.strategy.sequence_strategy;

import com.cygnus.ipoten.infrastructure.external.fastapi.client.FastApiFirstFollowupQuestionClientImpl;
import com.cygnus.ipoten.infrastructure.external.fastapi.request.FastApiFirstQuestionRequest;
import com.cygnus.ipoten.infrastructure.external.fastapi.response.FastApiQuestionResponse;
import com.cygnus.ipoten.interview.entity.AcademicBackground;
import com.cygnus.ipoten.interview.entity.CompanyNameMapping;
import com.cygnus.ipoten.interview.entity.ExperienceLevel;
import com.cygnus.ipoten.interview.entity.Interview;
import com.cygnus.ipoten.interview.entity.JobCategory;
import com.cygnus.ipoten.interview.service.InterviewService;
import com.cygnus.ipoten.interview.service.request.InterviewSequenceRequest;
import com.cygnus.ipoten.interview.service.response.InterviewProgressResponse;
import com.cygnus.ipoten.interviewQA.entity.InterviewQA;
import com.cygnus.ipoten.interviewQA.service.InterviewQAService;
import com.cygnus.ipoten.interviewee_profile.entity.IntervieweeProfile;
import com.cygnus.ipoten.interviewee_profile.service.IntervieweeProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("1")
@RequiredArgsConstructor
@Transactional
public class FirstQuestion implements InterviewSequenceStrategy {

    private static final Logger logger = LoggerFactory.getLogger(FirstQuestion.class);

    private final IntervieweeProfileService intervieweeProfileService;
    private final InterviewService interviewService;
    private final InterviewQAService interviewQAService;
    private final FastApiFirstFollowupQuestionClientImpl fastApiFirstFollowupQuestionClient;

    @Override
    public InterviewProgressResponse getQuestionByCompany(InterviewSequenceRequest interviewSequenceRequest, String userToken) {

        log.info("자기소개를 제외한 첫 번째 질문 시도");

        Interview interview = interviewService.findById(interviewSequenceRequest.getInterviewId())
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 인터뷰를 찾을 수 없습니다"));

        IntervieweeProfile intervieweeProfile = interview.getIntervieweeProfile();

        // Enum을 사용하여 문자열을 ID로 변환
        JobCategory jobCategory = JobCategory.fromString(intervieweeProfile.getJob());
        ExperienceLevel experienceLevel = ExperienceLevel.fromString(intervieweeProfile.getCareer());
        AcademicBackground academicBackground = AcademicBackground.fromString(intervieweeProfile.getMajor());
        
        // 회사 이름을 영문으로 변환
        String koreanCompanyName = intervieweeProfile.getCompany();
        String englishCompanyName = CompanyNameMapping.toEnglishName(koreanCompanyName);
        
        logger.info("회사 이름 변환: {} -> {}", koreanCompanyName, englishCompanyName);

        FastApiQuestionResponse fastApiFirstFollowupQuestion = fastApiFirstFollowupQuestionClient.getFastApiFirstFollowupQuestion(
                new FastApiFirstQuestionRequest(
                        interviewSequenceRequest.getInterviewId(),
                        jobCategory.getId(),                // Enum의 ID 사용
                        experienceLevel.getId(),            // Enum의 ID 사용
                        academicBackground.getId(),         // Enum의 ID 사용
                        englishCompanyName,                 // 변환된 영문 회사 이름 사용
                        interviewSequenceRequest.getInterviewQAId(),
                        interviewSequenceRequest.getAnswer(),
                        userToken
                )
        );
        String question = fastApiFirstFollowupQuestion.getQuestions().get(0);
        InterviewQA interviewQuestion = interviewQAService.createInterviewQuestion(interview,question);
        interviewQAService.saveInterviewQAByInterview(interview, interviewQuestion);

        return new InterviewProgressResponse(interviewSequenceRequest.getInterviewQAId()+1, interviewSequenceRequest.getInterviewId(), question);
    }
}
