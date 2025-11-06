package com.cygnus.ipoten.studyroom.service.response;

import com.cygnus.ipoten.studyroom.entity.StudyRoom;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class CreateStudyRoomResponse {
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

    public static CreateStudyRoomResponse from(StudyRoom studyRoom) {
        return new CreateStudyRoomResponse(
                studyRoom.getId(),
                studyRoom.getTitle(),
                studyRoom.getDescription(),
                studyRoom.getMaxMembers(),
                studyRoom.getStatus().name(),       // Enum을 String으로 변환
                studyRoom.getLocation().name(),     // Enum을 String으로 변환
                studyRoom.getStudyLevel().name(),
                studyRoom.getRecruitingRoles(),
                studyRoom.getSkillStack(),
                studyRoom.getCreatedAt()
        );
    }
}