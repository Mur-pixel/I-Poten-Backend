package com.cygnus.ipoten.awsCost.config;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "admin")
public record AdminAwsProps(
        String adminAwsAccessKey,
        String adminAwsSecretKey
) { }