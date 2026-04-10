package com.cygnus.iptn.authentication.social;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SocialLoginPopupResponseBuilder {

    // 객체를 JSON 문자열로 직렬화하기 위한 Jackson ObjectMapper
    private final ObjectMapper objectMapper;

    public String buildErrorHtml(SocialLoginErrorResponse errorResponse, String origin) {
        try {
            // 부모 창(window.opener)으로 전달할 payload 생성
            // postMessage 표준 형식에 맞춰 error 객체를 JSON 문자열로 직렬화
            String payload = objectMapper.writeValueAsString(Map.of("error", errorResponse));

            // 팝업 창에서 부모 창으로 메시지를 전달한 뒤 스스로 닫는 HTML 반환
            // origin 을 명시해서 허용된 부모 창에만 메시지를 전달하도록 제한
            return """
                    <html><body><script>
                    window.opener.postMessage(%s, '%s'); window.close();
                    </script></body></html>
                    """.formatted(payload, escape(origin));
        } catch (JsonProcessingException e) {
            // 에러 응답 객체를 JSON 으로 만들지 못하면 정상적인 프론트 전달이 불가능하므로
            // 런타임 예외로 감싸서 상위 계층에서 처리하도록 함
            throw new IllegalStateException("소셜 로그인 에러 응답 직렬화에 실패했습니다.", e);
        }
    }

    private String escape(String text) {
        // HTML/JS 문자열 내부에 작은따옴표가 들어갈 경우
        // 스크립트 문법 오류를 막기 위해 이스케이프 처리
        return text.replace("'", "\\'");
    }
}