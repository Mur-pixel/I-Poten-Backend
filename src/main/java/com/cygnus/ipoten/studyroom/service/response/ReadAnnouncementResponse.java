package com.cygnus.ipoten.studyroom.service.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.studyroom.entity.Announcement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ReadAnnouncementResponse {
    private final Long id;
    private final String title;
    private final String content;
    @JsonProperty("isPinned")
    private final boolean isPinned;
    private final LocalDateTime createdAt;
    private final AuthorResponse author;

    public static ReadAnnouncementResponse from(Announcement announcement) {
        return new ReadAnnouncementResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.isPinned(),
                announcement.getCreatedAt(),
                AuthorResponse.from(announcement.getAuthor())
        );
    }

    @Getter
    public static class AuthorResponse {
        private final Long id;
        private final String nickname;

        public AuthorResponse(Long id, String nickname) {
            this.id = id;
            this.nickname = nickname;
        }

        public static AuthorResponse from(AccountProfile author) {
            return new AuthorResponse(author.getId(), author.getNickname());
        }
    }
}