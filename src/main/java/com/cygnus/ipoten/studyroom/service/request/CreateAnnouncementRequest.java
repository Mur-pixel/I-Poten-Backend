package com.cygnus.ipoten.studyroom.service.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateAnnouncementRequest {
    private final Long studyRoomId;
    private final Long authorId;
    private final String title;
    private final String content;
}
