package com.cygnus.ipoten.studyApplication.controller.response_form;

import com.cygnus.ipoten.studyApplication.entity.ApplicationStatus;
import com.cygnus.ipoten.studyApplication.service.response.ListMyApplicationResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class ListMyApplicationResponseForm {
    private final Long id;
    private final ApplicationStatus status;
    private final String message;
    private final LocalDateTime appliedAt;
    private final StudyInfoForm study;

    public static ListMyApplicationResponseForm from(ListMyApplicationResponse response) {
        return new ListMyApplicationResponseForm(
                response.getId(),
                response.getStatus(),
                response.getMessage(),
                response.getAppliedAt(),
                StudyInfoForm.from(response.getStudy())
        );
    }

    @Getter
    @RequiredArgsConstructor
    private static class StudyInfoForm {
        private final Long id;
        private final String title;
        private final String location;
        private final Set<String> recruitingRoles;

        public static StudyInfoForm from(ListMyApplicationResponse.StudyInfo studyInfo) {
            return new StudyInfoForm(
                    studyInfo.getId(),
                    studyInfo.getTitle(),
                    studyInfo.getLocation(),
                    studyInfo.getRecruitingRoles()
            );
        }
    }
}