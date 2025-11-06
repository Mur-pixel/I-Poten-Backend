package com.cygnus.ipoten.studyroom.service.response;

import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.studyroom.entity.StudyRoom;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class ReadStudyRoomResponse {
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
    private final Integer currentMembers;
    private final Long hostId;
    private final String hostNickname;
    private final boolean isOwner;

    public static ReadStudyRoomResponse from(StudyRoom studyRoom, boolean isOwner) {
        if (studyRoom == null) {
            return null;
        }

        // host가 null일 경우를 대비하여 안전하게 ID를 가져옵니다.
        AccountProfile host = studyRoom.getHost();
        Long hostId = (host != null) ? host.getId() : null;
        String hostNickname = (host != null) ? host.getNickname() : null;

        // Enum 타입 필드가 null일 경우를 대비하여 안전하게 name()을 호출합니다.
        String status = (studyRoom.getStatus() != null) ? studyRoom.getStatus().name() : null;
        String location = (studyRoom.getLocation() != null) ? studyRoom.getLocation().getKoreanName() : null;
        String studyLevel = (studyRoom.getStudyLevel() != null) ? studyRoom.getStudyLevel().name() : null;

        return new ReadStudyRoomResponse(
                studyRoom.getId(),
                studyRoom.getTitle(),
                studyRoom.getDescription(),
                studyRoom.getMaxMembers(),
                status,
                location,
                studyLevel,
                studyRoom.getRecruitingRoles(),
                studyRoom.getSkillStack(),
                studyRoom.getCreatedAt(),
                studyRoom.getStudyMembers().size(),
                hostId,
                hostNickname,
                isOwner
        );
    }
}