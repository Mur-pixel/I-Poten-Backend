package com.cygnus.ipoten.studyroom.service.request;

import com.cygnus.ipoten.studyroom.entity.StudyLevel;
import com.cygnus.ipoten.studyroom.entity.StudyLocation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public class UpdateStudyRoomRequest {
    private final String title;
    private final String description;
    private final Integer maxMembers;
    private final StudyLocation location;
    private final StudyLevel studyLevel;
    private final Set<String> recruitingRoles; // 👈 List -> Set
    private final Set<String> skillStack;      // 👈 List -> Set

}