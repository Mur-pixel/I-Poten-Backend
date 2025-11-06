package com.cygnus.ipoten.interview.service.strategy.sequence_strategy;

import com.cygnus.ipoten.account_project.entity.AccountProject;
import com.cygnus.ipoten.account_project.service.AccountProjectService;
import com.cygnus.ipoten.infrastructure.external.fastapi.client.FastApiSecondFollowupQuestionClient;
import com.cygnus.ipoten.infrastructure.external.fastapi.request.FastApiSecondProgressRequest;
import com.cygnus.ipoten.infrastructure.external.fastapi.response.FastApiQuestionResponse;
import com.cygnus.ipoten.interview.entity.Interview;
import com.cygnus.ipoten.interview.service.InterviewService;
import com.cygnus.ipoten.interview.service.request.InterviewSequenceRequest;
import com.cygnus.ipoten.infrastructure.external.fastapi.request.FastApiAccountProjectRequest;
import com.cygnus.ipoten.interview.service.response.InterviewProgressResponse;
import com.cygnus.ipoten.interviewQA.entity.InterviewQA;
import com.cygnus.ipoten.interviewQA.service.InterviewQAService;
import com.cygnus.ipoten.interviewee_profile.entity.IntervieweeProfile;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component("2")
@RequiredArgsConstructor
@Transactional
public class SecondQuestion implements InterviewSequenceStrategy {

    private final InterviewService interviewService;
    private final AccountProjectService accountProjectService;
    private final RedisCacheService redisCacheService;
    private final FastApiSecondFollowupQuestionClient fastApiSecondFollowupQuestionClient;
    private final InterviewQAService interviewQAService;


    @Override
    public InterviewProgressResponse getQuestionByCompany(InterviewSequenceRequest interviewSequenceRequest, String userToken) {

        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        if (accountId == null) {
            new IllegalArgumentException("두 번째 질문에서 해당 유저를 찾을 수 없습니다");
        }

        log.info("두 번째 질문 시도");

        Interview interview = interviewService.findById(interviewSequenceRequest.getInterviewId())
                .orElseThrow(() -> new IllegalArgumentException("두 번째 질문에서 인터뷰 정보를 찾을 수 없습니다."));

        IntervieweeProfile intervieweeProfile = interview.getIntervieweeProfile();
        boolean projectExp = intervieweeProfile.isProjectExp();


        FastApiQuestionResponse fastApiSecondFollowupQuestion;

        if (projectExp) {
            List<AccountProject> allByAccountIdAndIsActiveTrue =
                    accountProjectService.findAllByAccount_IdAndIsActiveTrue(accountId);

            List<FastApiAccountProjectRequest> responseList = allByAccountIdAndIsActiveTrue.stream()
                    .map(project -> new FastApiAccountProjectRequest(
                            project.getProjectName(),
                            project.getProjectDescription()
                    ))
                    .collect(Collectors.toList());

            fastApiSecondFollowupQuestion = fastApiSecondFollowupQuestionClient.getFastApiSecondFollowupQuestion(
                    FastApiSecondProgressRequest.builder()
                            .userToken(userToken)
                            .interviewId(interview.getId())
                            .projectExperience(2)
                            .questionId(interviewSequenceRequest.getInterviewQAId())
                            .projectResponses(responseList)
                            .build()
            );
        } else {
            fastApiSecondFollowupQuestion = fastApiSecondFollowupQuestionClient.getFastApiSecondFollowupQuestion(
                    FastApiSecondProgressRequest.builder()
                            .userToken(userToken)
                            .interviewId(interview.getId())
                            .projectExperience(1)
                            .questionId(interviewSequenceRequest.getInterviewQAId())
                            .build()
            );
        }
        Long interviewQAId = interviewSequenceRequest.getInterviewQAId();
        String question = fastApiSecondFollowupQuestion.getQuestions().get(0);
        interviewQAService.saveInterviewAnswer(interviewQAId, interviewSequenceRequest.getAnswer());
        InterviewQA interviewQuestion = interviewQAService.createInterviewQuestion(interview, question);
        log.info("두 번째 질문 로직에서 세 번째 질문 만든거 아이디 : {}", interviewQuestion.getId());

        return new InterviewProgressResponse(
                interviewQuestion.getId(),
                interview.getId(),
                question
        );
    }
}
