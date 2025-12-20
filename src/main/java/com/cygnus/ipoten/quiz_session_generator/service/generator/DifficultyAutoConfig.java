package com.cygnus.ipoten.quiz_session_generator.service.generator;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DifficultyProperties.class)
public class DifficultyAutoConfig { }