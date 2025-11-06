package com.cygnus.ipoten.studyschedule.service.response;

import com.cygnus.ipoten.studyschedule.entity.StudySchedule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListUserStudyScheduleResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long studyRoomId;
    private String studyRoomTitle;

    public static ListUserStudyScheduleResponse from(StudySchedule schedule) {
        return new ListUserStudyScheduleResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getStudyRoom().getId(),
                schedule.getStudyRoom().getTitle()
        );
    }
}
