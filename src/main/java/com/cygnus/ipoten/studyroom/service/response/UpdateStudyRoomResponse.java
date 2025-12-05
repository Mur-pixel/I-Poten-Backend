package com.cygnus.ipoten.studyroom.service.response;

import com.cygnus.ipoten.studyroom.entity.StudyRoom;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class UpdateStudyRoomResponse {
    private final Long id;
    private final String title;
    private final String description;
    private final Integer maxMembers;
    private final String status;
    private final String location;
    private final String studyLevel;
    private final Set<String> recruitingRoles; // 👈 List -> Set
    private final Set<String> skillStack;      // 👈 List -> Set
    private final LocalDateTime createdAt;
    private final Long hostId;

    public static UpdateStudyRoomResponse from(StudyRoom studyRoom) {
        return new UpdateStudyRoomResponse(
                studyRoom.getId(),
                studyRoom.getTitle(),
                studyRoom.getDescription(),
                studyRoom.getMaxMembers(),
                studyRoom.getStatus().name(),
                studyRoom.getLocation().name(),
                studyRoom.getStudyLevel().name(),
                studyRoom.getRecruitingRoles(),
                studyRoom.getSkillStack(),
                studyRoom.getCreatedAt(),
                studyRoom.getHost().getId()
        );
    }
}