package com.cygnus.ipoten.report.service.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UploadUrlResponse {
    private final String uploadUrl;
    private final String attachmentS3Key;
}
