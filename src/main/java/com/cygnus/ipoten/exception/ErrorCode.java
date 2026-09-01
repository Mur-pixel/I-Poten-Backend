package com.cygnus.ipoten.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 전역 표준 에러 코드.
 *
 * <p>도메인별로 prefix 를 두어 클라이언트가 코드만으로 분기 가능하도록 설계.
 * 새로운 도메인 에러 추가 시 이 enum 에 등록 후 {@link BusinessException} 으로 던진다.
 *
 * <pre>
 *  prefix:
 *    COMMON_*     : 공통
 *    AUTH_*       : 인증/인가
 *    ACCOUNT_*    : 사용자 계정
 *    WORDBOOK_*   : 단어장/단어
 *    QUIZ_*       : 퀴즈
 *    INTERVIEW_*  : 면접
 *    CREDIT_*     : 크레딧/결제
 *    EXTERNAL_*   : 외부 API (소셜로그인, OpenAI 등)
 * </pre>
 */
@Getter
public enum ErrorCode {

    // ── 공통 ───────────────────────────────────────────────────
    COMMON_INVALID_INPUT(HttpStatus.BAD_REQUEST,           "COMMON_001", "잘못된 요청 입력입니다."),
    COMMON_RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND,        "COMMON_002", "요청한 리소스를 찾을 수 없습니다."),
    COMMON_METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED,"COMMON_003", "허용되지 않은 HTTP 메서드입니다."),
    COMMON_CONFLICT(HttpStatus.CONFLICT,                   "COMMON_004", "요청이 현재 리소스 상태와 충돌합니다."),
    COMMON_PAYMENT_REQUIRED(HttpStatus.PAYMENT_REQUIRED,   "COMMON_005", "결제가 필요합니다."),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"COMMON_999", "서버 내부 오류가 발생했습니다."),

    // ── 인증/인가 ─────────────────────────────────────────────
    AUTH_NOT_LOGGED_IN(HttpStatus.UNAUTHORIZED,            "AUTH_001",   "로그인이 필요합니다."),
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED,            "AUTH_002",   "토큰이 유효하지 않거나 만료되었습니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN,                   "AUTH_003",   "권한이 없습니다."),
    AUTH_INTERNAL_KEY_INVALID(HttpStatus.UNAUTHORIZED,     "AUTH_004",   "내부 API 키가 유효하지 않습니다."),

    // ── 계정 ───────────────────────────────────────────────────
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND,                "ACCOUNT_001","계정을 찾을 수 없습니다."),
    ACCOUNT_DUPLICATE_NICKNAME(HttpStatus.CONFLICT,        "ACCOUNT_002","이미 사용 중인 닉네임입니다."),
    ACCOUNT_REJOIN_BLOCKED(HttpStatus.FORBIDDEN,           "ACCOUNT_003","탈퇴 후 재가입 대기 기간 내에는 가입할 수 없습니다."),

    // ── 단어장 ────────────────────────────────────────────────
    WORDBOOK_NOT_FOUND(HttpStatus.NOT_FOUND,               "WORDBOOK_001","단어장을 찾을 수 없습니다."),
    WORDBOOK_DUPLICATE_NAME(HttpStatus.CONFLICT,           "WORDBOOK_002","이미 사용 중인 단어장 이름입니다."),
    WORDBOOK_TERM_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST,   "WORDBOOK_003","단어장에 담을 수 있는 단어 수를 초과했습니다."),
    TERM_NOT_FOUND(HttpStatus.NOT_FOUND,                   "WORDBOOK_004","단어를 찾을 수 없습니다."),

    // ── 면접/퀴즈 ─────────────────────────────────────────────
    INTERVIEW_NOT_FOUND(HttpStatus.NOT_FOUND,              "INTERVIEW_001","면접을 찾을 수 없습니다."),
    QUIZ_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND,           "QUIZ_001",   "퀴즈 세션을 찾을 수 없습니다."),

    // ── 크레딧 ────────────────────────────────────────────────
    CREDIT_INSUFFICIENT(HttpStatus.PAYMENT_REQUIRED,       "CREDIT_001", "보유 크레딧이 부족합니다."),
    CREDIT_WALLET_NOT_FOUND(HttpStatus.NOT_FOUND,          "CREDIT_002", "크레딧 지갑이 존재하지 않습니다."),

    // ── 외부 API ──────────────────────────────────────────────
    EXTERNAL_SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED,  "EXTERNAL_001","소셜 로그인에 실패했습니다."),
    EXTERNAL_GOOGLE_TOKEN_FAILED(HttpStatus.UNAUTHORIZED,  "EXTERNAL_002","Google 액세스 토큰 발급에 실패했습니다."),
    EXTERNAL_GOOGLE_USERINFO_FAILED(HttpStatus.UNAUTHORIZED,"EXTERNAL_003","Google 사용자 정보 조회에 실패했습니다."),
    EXTERNAL_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT,       "EXTERNAL_004","외부 API 응답이 시간 초과되었습니다.");

    private final HttpStatus status;
    private final String     code;
    private final String     defaultMessage;

    ErrorCode(HttpStatus status, String code, String defaultMessage) {
        this.status = status;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
