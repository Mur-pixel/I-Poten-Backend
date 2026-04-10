package com.cygnus.ipoten.config;

import com.cygnus.ipoten.ebook.service.EbookProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EbookProperties.class)
public class EbookConfig {
}
