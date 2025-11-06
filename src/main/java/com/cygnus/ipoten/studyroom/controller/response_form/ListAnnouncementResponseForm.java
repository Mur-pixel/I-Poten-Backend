package com.cygnus.ipoten.studyroom.controller.response_form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.cygnus.ipoten.studyroom.service.response.ListAnnouncementResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ListAnnouncementResponseForm {
    private final Long id;
    private final String title;
    @JsonProperty("isPinned")
    private final boolean isPinned;
    private final LocalDateTime createdAt;
    private final AuthorResponseForm author;

    public static ListAnnouncementResponseForm from(ListAnnouncementResponse response) {
        return new ListAnnouncementResponseForm(
                response.getId(),
                response.getTitle(),
                response.isPinned(),
                response.getCreatedAt(),
                AuthorResponseForm.from(response.getAuthor()) // AuthorResponseForm 재사용
        );
    }

}