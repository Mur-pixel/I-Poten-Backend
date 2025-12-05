package com.cygnus.ipoten.report.service.request;

import com.cygnus.ipoten.report.entity.ReportStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateReportStatusRequest {
    private final ReportStatus status;
}
