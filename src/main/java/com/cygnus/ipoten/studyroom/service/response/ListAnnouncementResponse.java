package com.cygnus.ipoten.studyroom.service.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.studyroom.entity.Announcement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ListAnnouncementResponse {
    private final Long id;
    private final String title;
    @JsonProperty("isPinned")
    private final boolean isPinned;
    private final LocalDateTime createdAt;
    private final AuthorResponse author;

    // 엔티티를 서비스 응답 객체로 변환
    public static ListAnnouncementResponse from(Announcement announcement) {
        return new ListAnnouncementResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.isPinned(),
                announcement.getCreatedAt(),
                AuthorResponse.from(announcement.getAuthor())
        );
    }

    // 작성자 정보를 담는 내부 클래스
    @Getter
    public static class AuthorResponse {
        private final Long id;
        private final String nickname;

        // 생성자도 public으로 변경해주는 것이 좋습니다.
        public AuthorResponse(Long id, String nickname) {
            this.id = id;
            this.nickname = nickname;
        }

        public static AuthorResponse from(AccountProfile author) {
            return new AuthorResponse(author.getId(), author.getNickname());
        }
    }
}