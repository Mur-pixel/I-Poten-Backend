package com.cygnus.iptn.infrastructure.external.fastapi.client;

import com.cygnus.iptn.infrastructure.external.fastapi.response.FastApiQuestionResponse;
import com.cygnus.iptn.interview.controller.request.InterviewEndRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class FastApiEndInterviewImpl implements FastApiEndInterview {

    private final RestTemplate restTemplate;

    @Value("${fastapi.uri}")
    private String fastApiUri;

    @Override
    public void endInterview(InterviewEndRequest request) {
        try {
            restTemplate.postForObject(
                    fastApiUri + "/interview/question/end_interview", // URL
                    request, // 요청 바디
                    FastApiQuestionResponse.class // 응답 타입
            );
        } catch (Exception ex) {
            throw new RuntimeException("FastAPI 요청 중 오류 발생", ex);
        }
    }
}
