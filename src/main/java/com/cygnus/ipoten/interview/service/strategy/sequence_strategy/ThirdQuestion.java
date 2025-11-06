package com.cygnus.ipoten.interview.service.strategy.sequence_strategy;

import com.cygnus.ipoten.account_project.entity.AccountProject;
import com.cygnus.ipoten.account_project.service.AccountProjectService;
import com.cygnus.ipoten.infrastructure.external.fastapi.client.FastApiThirdFollowupQuestionClient;
import com.cygnus.ipoten.infrastructure.external.fastapi.request.FastApiAccountProjectRequest;
import com.cygnus.ipoten.infrastructure.external.fastapi.request.FastApiThirdProgressRequest;
import com.cygnus.ipoten.infrastructure.external.fastapi.response.FastApiQuestionResponse;
import com.cygnus.ipoten.interview.entity.*;
import com.cygnus.ipoten.interview.service.InterviewService;
import com.cygnus.ipoten.interview.service.request.InterviewSequenceRequest;
import com.cygnus.ipoten.interview.service.response.InterviewProgressResponse;
import com.cygnus.ipoten.interviewQA.entity.InterviewQA;
import com.cygnus.ipoten.interviewQA.service.InterviewQAService;
import com.cygnus.ipoten.interviewee_profile.entity.IntervieweeProfile;
import com.cygnus.ipoten.interviewee_profile.entity.TechStack;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component("3")
@RequiredArgsConstructor
@Transactional
public class ThirdQuestion implements InterviewSequenceStrategy{

    private final InterviewService interviewService;
    private final RedisCacheService redisCacheService;
    private final FastApiThirdFollowupQuestionClient fastApiThirdFollowupQuestionClient;
    private final InterviewQAService interviewQAService;
    private final AccountProjectService accountProjectService;


    @Override
    public InterviewProgressResponse getQuestionByCompany(InterviewSequenceRequest interviewSequenceRequest, String userToken) {

        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        if (accountId == null) {
            new IllegalArgumentException("두 번째 질문에서 해당 유저를 찾을 수 없습니다");
        }


        Interview interview = interviewService.findById(interviewSequenceRequest.getInterviewId())
                .orElseThrow(() -> new IllegalArgumentException("두 번째 질문에서 인터뷰 정보를 찾을 수 없습니다."));

        IntervieweeProfile intervieweeProfile = interview.getIntervieweeProfile();
        boolean projectExp = intervieweeProfile.isProjectExp();

        FastApiQuestionResponse fastApiThirdFollowupQuestion;

        List<TechStack> techStack = intervieweeProfile.getTechStack();

        // Enum을 사용하여 문자열을 ID로 변환
        JobCategory jobCategory = JobCategory.fromString(intervieweeProfile.getJob());
        ExperienceLevel experienceLevel = ExperienceLevel.fromString(intervieweeProfile.getCareer());
        AcademicBackground academicBackground = AcademicBackground.fromString(intervieweeProfile.getMajor());

        // 회사 이름을 영문으로 변환
        String koreanCompanyName = intervieweeProfile.getCompany();
        String englishCompanyName = CompanyNameMapping.toEnglishName(koreanCompanyName);

        if (projectExp) {
            List<AccountProject> allByAccountIdAndIsActiveTrue =
                    accountProjectService.findAllByAccount_IdAndIsActiveTrue(accountId);

            List<FastApiAccountProjectRequest> responseList = allByAccountIdAndIsActiveTrue.stream()
                    .map(project -> new FastApiAccountProjectRequest(
                            project.getProjectName(),
                            project.getProjectDescription()
                    ))
                    .collect(Collectors.toList());

            fastApiThirdFollowupQuestion = fastApiThirdFollowupQuestionClient.getFastApiThirdFollowupQuestion(
                    FastApiThirdProgressRequest.builder()
                            .userToken(userToken)
                            .interviewId(interview.getId())
                            .projectExperience(2)
                            .topic(jobCategory.getId())
                            .experienceLevel(experienceLevel.getId())
                            .academicBackground(academicBackground.getId())
                            .questionId(interviewSequenceRequest.getInterviewQAId())
                            .projectResponses(responseList)
                            .answerText(interviewSequenceRequest.getAnswer())
                            .techStack(techStack)
                            .companyName(englishCompanyName)
                            .build()
            );
        } else {
            fastApiThirdFollowupQuestion = fastApiThirdFollowupQuestionClient.getFastApiThirdFollowupQuestion(
                    FastApiThirdProgressRequest.builder()
                            .userToken(userToken)
                            .interviewId(interview.getId())
                            .projectExperience(1)
                            .questionId(interviewSequenceRequest.getInterviewQAId())
                            .topic(jobCategory.getId())
                            .experienceLevel(experienceLevel.getId())
                            .academicBackground(academicBackground.getId())
                            .answerText(interviewSequenceRequest.getAnswer())
                            .techStack(techStack)
                            .companyName(englishCompanyName)
                            .build()
            );
        }
        Long interviewQAId = interviewSequenceRequest.getInterviewQAId();
        String question = fastApiThirdFollowupQuestion.getQuestions().get(0);
        interviewQAService.saveInterviewAnswer(interviewQAId, interviewSequenceRequest.getAnswer());
        InterviewQA interviewQuestion = interviewQAService.createInterviewQuestion(interview, question);

        return new InterviewProgressResponse(
                interviewQuestion.getId(),
                interview.getId(),
                question
        );
    }
}
