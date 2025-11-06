package com.cygnus.ipoten.studyroom.controller.response_form;

import com.cygnus.ipoten.studyroom.service.response.UpdateStudyRoomResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class UpdateStudyRoomResponseForm {
    private final Long id;
    private final String title;
    private final String description;
    private final Integer maxMembers;
    private final String status;
    private final String location;
    private final Set<String> recruitingRoles; // 👈 List -> Set
    private final Set<String> skillStack;      // 👈 List -> Set
    private final LocalDateTime createdAt;
    private final Long hostId;

    public static UpdateStudyRoomResponseForm from(UpdateStudyRoomResponse response) {
        return new UpdateStudyRoomResponseForm(
                response.getId(),
                response.getTitle(),
                response.getDescription(),
                response.getMaxMembers(),
                response.getStatus(),
                response.getLocation(),
                response.getRecruitingRoles(),
                response.getSkillStack(),
                response.getCreatedAt(),
                response.getHostId()
        );
    }
}
