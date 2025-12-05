package com.cygnus.ipoten.studyroom.service.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateAnnouncementRequest {
    private final String title;
    private final String content;
}
