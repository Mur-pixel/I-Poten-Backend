package com.cygnus.ipoten.report.service.request;

import com.cygnus.ipoten.report.entity.ReportCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateReportRequest {
    private final Long reportedUserId;
    private final Long studyRoomId;
    private final ReportCategory category;
    private final String description;
    private final String attachmentS3Key;
}
