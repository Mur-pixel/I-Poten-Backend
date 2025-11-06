package com.cygnus.ipoten.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ReportS3Service {

    @Qualifier("reportS3Presigner")
    private final S3Presigner reportS3Presigner;

    @Value("${cloud.aws.s3.bucket.report-files}")
    private String reportBucket;

    public String generateUploadUrl(String key, String contentType) {

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(reportBucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(objectRequest)
                .signatureDuration(Duration.ofMinutes(10))
                .build();

        PresignedPutObjectRequest presignedRequest = reportS3Presigner.presignPutObject(presignRequest);

        return presignedRequest.url().toString();
    }
}