package com.cygnus.ipoten.studyroom.controller.response_form;

import com.cygnus.ipoten.studyroom.service.response.ReadStudyRoomResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class ReadStudyRoomResponseForm {
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

    public static ReadStudyRoomResponseForm from(ReadStudyRoomResponse response) {
        return new ReadStudyRoomResponseForm(
                response.getId(),
                response.getTitle(),
                response.getDescription(),
                response.getMaxMembers(),
                response.getStatus(),
                response.getLocation(),
                response.getStudyLevel(),
                response.getRecruitingRoles(),
                response.getSkillStack(),
                response.getCreatedAt(),
                response.getCurrentMembers(),
                response.getHostId(),
                response.getHostNickname(),
                response.isOwner()
        );
    }
//    public static ReadStudyRoomResponseForm from(StudyRoom studyRoom) {
//        if (studyRoom == null) {
//            return null;
//        }
//
//        AccountProfile host = studyRoom.getHost();
//        Long hostId = (host != null) ? host.getId() : null;
//        String hostNickname = (host != null) ? host.getNickname() : null;
//
//        String status = (studyRoom.getStatus() != null) ? studyRoom.getStatus().name() : null;
//        String location = (studyRoom.getLocation() != null) ? studyRoom.getLocation().name() : null;
//        String studyLevel = (studyRoom.getStudyLevel() != null) ? studyRoom.getStudyLevel().name() : null;
//
//        return new ReadStudyRoomResponseForm(
//                studyRoom.getId(),
//                studyRoom.getTitle(),
//                studyRoom.getDescription(),
//                studyRoom.getMaxMembers(),
//                status,
//                location,
//                studyLevel,
//                studyRoom.getRecruitingRoles(),
//                studyRoom.getSkillStack(),
//                studyRoom.getCreatedAt(),
//                studyRoom.getCurrentMembers(),
//                hostId,
//                hostNickname,
//        );
//    }
}