package com.cygnus.ipoten.awsCost.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;

@Configuration
@EnableConfigurationProperties(AdminAwsProps.class)
public class AdminAwsConfig {

    @Bean
    @Qualifier("adminCostExplorerClient")
    public CostExplorerClient adminCostExplorerClient(AdminAwsProps props) {
        // 키 검증
        if (props.adminAwsAccessKey() == null || props.adminAwsAccessKey().isBlank()
                || props.adminAwsSecretKey() == null || props.adminAwsSecretKey().isBlank()) {
            throw new IllegalStateException("[admin] AWS access/secret 키가 설정되지 않았습니다. " +
                    "ENV ADMINISTRATOR_AWS_ACCESS_KEY / ADMINISTRATOR_AWS_SECRET_KEY 를 확인하세요.");
        }

        // Basic credentials만 사용
        AwsCredentialsProvider provider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.adminAwsAccessKey(), props.adminAwsSecretKey())
        );

        // CE는 us-east-1
        return CostExplorerClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(provider)
                .build();
    }
}
