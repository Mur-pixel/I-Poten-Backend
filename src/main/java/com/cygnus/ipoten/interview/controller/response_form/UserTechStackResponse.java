package com.cygnus.ipoten.interview.controller.response_form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserTechStackResponse {
    private boolean hasInterview;         // 인터뷰 완료 여부
    private String job;                   // 면접 시 선택한 직무
    private List<TechStackDto> techStacks; // 면접 시 선택한 기술 스택 목록
    private String message;               // 안내 문구 (인터뷰 없을 경우)
    private String createdAt;              // 최근 완료된 인터뷰 날짜

    @Getter
    @AllArgsConstructor
    public static class TechStackDto {
        private String key;               // TechStack Enum의 key (e.g. "JAVA")
        private String displayName;       // Enum의 displayName (e.g. "Java")
    }

    public static UserTechStackResponse noInterview() {
        return UserTechStackResponse.builder()
                .hasInterview(false)
                .message("아직 AI 면접을 진행하지 않았습니다.")
                .build();
    }
}
