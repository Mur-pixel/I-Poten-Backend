package com.cygnus.ipoten.studyroom.controller.response_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.cygnus.ipoten.studyroom.service.response.ReadAnnouncementResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ReadAnnouncementResponseForm {
    private final Long id;
    private final String title;
    private final String content;
    @JsonProperty("isPinned")
    private final boolean isPinned;
    private final LocalDateTime createdAt;
    private final AuthorResponseForm author;

    public static ReadAnnouncementResponseForm from(ReadAnnouncementResponse response) {
        return new ReadAnnouncementResponseForm(
                response.getId(),
                response.getTitle(),
                response.getContent(),
                response.isPinned(),
                response.getCreatedAt(),
                AuthorResponseForm.from(response.getAuthor())
        );
    }
}
