package com.cygnus.ipoten.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorResponse 직렬화/팩토리 테스트")
class ErrorResponseTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("of() 는 ErrorCode 의 status/code/message 를 그대로 채운다")
    void factoryOf() {
        ErrorResponse r = ErrorResponse.of(ErrorCode.WORDBOOK_NOT_FOUND, "/foo", "trace");
        assertThat(r.code()).isEqualTo("WORDBOOK_001");
        assertThat(r.message()).isEqualTo("단어장을 찾을 수 없습니다.");
        assertThat(r.status()).isEqualTo(404);
        assertThat(r.path()).isEqualTo("/foo");
        assertThat(r.traceId()).isEqualTo("trace");
        assertThat(r.timestamp()).isNotNull();
        assertThat(r.errors()).isNull();
    }

    @Test
    @DisplayName("withFieldErrors 는 errors 가 채워진다")
    void factoryWithFieldErrors() {
        ErrorResponse r = ErrorResponse.withFieldErrors(
                ErrorCode.COMMON_INVALID_INPUT, "/x", "t",
                List.of(new ErrorResponse.FieldErrorDetail("name", "blank")));
        assertThat(r.errors()).hasSize(1);
        assertThat(r.errors().get(0).field()).isEqualTo("name");
    }

    @Test
    @DisplayName("errors==null 이면 JSON 에서 누락된다 (NON_NULL)")
    void serializeOmitsNullErrors() throws Exception {
        ErrorResponse r = ErrorResponse.of(ErrorCode.WORDBOOK_NOT_FOUND, "/x", "t");
        String json = mapper.writeValueAsString(r);
        assertThat(json).doesNotContain("\"errors\"");
        assertThat(json).contains("\"code\":\"WORDBOOK_001\"");
    }
}
