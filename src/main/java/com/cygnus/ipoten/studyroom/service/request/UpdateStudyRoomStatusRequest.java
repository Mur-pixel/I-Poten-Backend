package com.cygnus.ipoten.studyroom.service.request;

import com.cygnus.ipoten.studyroom.entity.StudyStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateStudyRoomStatusRequest {
    private final StudyStatus status;
}