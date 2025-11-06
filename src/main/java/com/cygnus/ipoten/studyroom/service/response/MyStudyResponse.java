package com.cygnus.ipoten.studyroom.service.response;

import com.cygnus.ipoten.studyroom.entity.StudyRoom;
import com.cygnus.ipoten.studyroom.entity.StudyStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class MyStudyResponse {
    private final Long id;
    private final String title;
    private final StudyStatus status;
    private final Integer currentMembers;
    private final Integer maxMembers;
    private final String location;
    private final Set<String> recruitingRoles;
    private final List<String> skillStack;

    public static MyStudyResponse from(StudyRoom studyRoom) {
        return new MyStudyResponse(
                studyRoom.getId(),
                studyRoom.getTitle(),
                studyRoom.getStatus(),
                studyRoom.getStudyMembers().size(),
                studyRoom.getMaxMembers(),
                studyRoom.getLocation().getKoreanName(),
                studyRoom.getRecruitingRoles(),
                new ArrayList<>(studyRoom.getSkillStack())
        );
    }
}