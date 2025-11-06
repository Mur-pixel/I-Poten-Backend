package com.cygnus.ipoten.studyApplication.service.response;

import com.cygnus.ipoten.studyApplication.entity.ApplicationStatus;
import com.cygnus.ipoten.studyApplication.entity.StudyApplication;
import com.cygnus.ipoten.studyroom.entity.StudyRoom;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class ListMyApplicationResponse {
    private final Long id;
    private final ApplicationStatus status;
    private final String message;
    private final LocalDateTime appliedAt;
    private final StudyInfo study;

    public static ListMyApplicationResponse from(StudyApplication application) {
        return new ListMyApplicationResponse(
                application.getId(),
                application.getStatus(),
                application.getMessage(),
                application.getAppliedAt(),
                StudyInfo.from(application.getStudyRoom())
        );
    }

    @Getter
    @RequiredArgsConstructor
    public static class StudyInfo {
        private final Long id;
        private final String title;
        private final String location;
        private final Set<String> recruitingRoles;

        public static StudyInfo from(StudyRoom studyRoom) {
            return new StudyInfo(
                    studyRoom.getId(),
                    studyRoom.getTitle(),
                    studyRoom.getLocation().name(),
                    studyRoom.getRecruitingRoles()
            );
        }
    }
}