package com.cygnus.ipoten.studyroom.service.response;

import com.cygnus.ipoten.studyroom.entity.StudyMember;
import com.cygnus.ipoten.studyroom.entity.StudyRole;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StudyMemberResponse {
    private final Long accountProfileId;
    private final String nickname;
    private final StudyRole role;

    public static StudyMemberResponse fromEntity(StudyMember studyMember) {
        return new StudyMemberResponse(
                studyMember.getAccountProfile().getId(),
                studyMember.getAccountProfile().getNickname(),
                studyMember.getRole()
        );
    }
}