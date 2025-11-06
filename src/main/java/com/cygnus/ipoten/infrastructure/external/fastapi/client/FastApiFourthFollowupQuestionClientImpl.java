package com.cygnus.ipoten.infrastructure.external.fastapi.client;

import com.cygnus.ipoten.infrastructure.external.fastapi.request.FastApiFourthProgressRequest;
import com.cygnus.ipoten.infrastructure.external.fastapi.response.FastApiQuestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class FastApiFourthFollowupQuestionClientImpl implements FastApiFourthFollowupQuestionClient {

    private final RestTemplate restTemplate;

    @Value("${fastapi.uri}")
    private String fastApiUri;

    // 세 번째 질문 프로젝트 꼬리 질문
    @Override
    public FastApiQuestionResponse getFastApiFourthFollowupQuestion(FastApiFourthProgressRequest request) {
        try {
            return restTemplate.postForObject(
                    fastApiUri + "/interview/question/tech-followup-generate", // URL
                    request, // 요청 바디
                    FastApiQuestionResponse.class // 응답 타입
            );
        } catch (Exception ex) {
            throw new RuntimeException("FastAPI 요청 중 오류 발생", ex);
        }
    }
}
