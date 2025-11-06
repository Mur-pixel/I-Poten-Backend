package com.cygnus.ipoten.studyschedule.service.response;

import com.cygnus.ipoten.studyschedule.entity.StudySchedule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ReadStudyScheduleResponse {
    private final Long id;
    private final Long authorId;
    private final String authorNickname;
    private final String title;
    private final String description;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    public static ReadStudyScheduleResponse from(StudySchedule schedule) {
        return new ReadStudyScheduleResponse(
                schedule.getId(),
                schedule.getAuthor().getId(),
                schedule.getAuthor().getNickname(),
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getStartTime(),
                schedule.getEndTime()
        );
    }
}