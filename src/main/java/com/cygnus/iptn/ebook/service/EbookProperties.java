package com.cygnus.iptn.ebook.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ebook 관련 설정값 바인딩용
 * - application.yml 의 `ebook.storage-dir` 값을 주입받음
 */
@ConfigurationProperties(prefix = "ebook")
public record EbookProperties(String storageDir) {
}
