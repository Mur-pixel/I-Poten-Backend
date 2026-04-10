package com.cygnus.iptn.quiz_session_generator.service.generator;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 난이도별 동작 프로파일을 외부 설정(application.yml)로 바인딩합니다.
 * 미설정 시 코드의 기본값(EASY/MEDIUM/HARD)이 사용됩니다.
 */
@ConfigurationProperties(prefix = "quiz.difficulty")
@Data
public class DifficultyProperties {

    /** 개별 프로파일 (yml에서 quiz.difficulty.easy.* 처럼 바인딩) */
    private Profile easy   = Profile.defaultsEasy();
    private Profile medium = Profile.defaultsMedium();
    private Profile hard   = Profile.defaultsHard();

    /**
     * difficulty 문자열(EASY/MEDIUM/HARD)을 받아 프로파일을 반환.
     * null/알 수 없는 값이면 MEDIUM을 돌려줍니다.
     */
    public Profile getProfileOrDefault(String difficulty) {
        if (difficulty == null) return medium;
        return switch (difficulty.trim().toUpperCase()) {
            case "EASY"   -> easy != null ? easy : Profile.defaultsEasy();
            case "HARD"   -> hard != null ? hard : Profile.defaultsHard();
            default       -> medium != null ? medium : Profile.defaultsMedium();
        };
    }

    @Data
    public static class Profile {
        /** 객관식 보기 개수(정답 포함) */
        private int optionCount = 4;

        /** 보기 길이 편향 허용 비율(0~1). 클수록 길이 차이가 커도 허용 */
        private double lengthBiasTolerance = 0.35;

        /** 지문 부정화 확률(0~1) */
        private double negateProb = 0.10;

        /** 표현 치환 확률(0~1) */
        private double replaceProb = 0.10;

        /** INITIALS 힌트: 정답 글자수 공개 여부 */
        private boolean revealLength = true;

        /** INITIALS 힌트: 초성 공개 개수(앞에서 n자) */
        private int revealInitialsCount = 0;

        /** 오답 유사도 하한(자카드 유사도) */
        private double similarityMin = 0.00;

        /** 오답 유사도 상한(자카드 유사도) */
        private double similarityMax = 0.60;

        /** 오답 후보 최대 개수(정답 제외). optionCount-1과 함께 사용됨 */
        private int maxDistractors = 6;

        /** 기본값 셋(EASY) */
        public static Profile defaultsEasy() {
            Profile p = new Profile();
            p.optionCount = 3;
            p.lengthBiasTolerance = 0.50;
            p.negateProb = 0.00;
            p.replaceProb = 0.05;
            p.revealLength = true;
            p.revealInitialsCount = 2;
            p.similarityMin = 0.10;
            p.similarityMax = 0.50;
            p.maxDistractors = 6;
            return p;
        }

        /** 기본값 셋(MEDIUM) */
        public static Profile defaultsMedium() {
            Profile p = new Profile();
            p.optionCount = 4;
            p.lengthBiasTolerance = 0.35;
            p.negateProb = 0.10;
            p.replaceProb = 0.10;
            p.revealLength = true;
            p.revealInitialsCount = 1;
            p.similarityMin = 0.05;
            p.similarityMax = 0.60;
            p.maxDistractors = 8;
            return p;
        }

        /** 기본값 셋(HARD) */
        public static Profile defaultsHard() {
            Profile p = new Profile();
            p.optionCount = 5;
            p.lengthBiasTolerance = 0.25;
            p.negateProb = 0.20;
            p.replaceProb = 0.20;
            p.revealLength = false;
            p.revealInitialsCount = 0;
            p.similarityMin = 0.00;
            p.similarityMax = 0.70;
            p.maxDistractors = 10;
            return p;
        }
    }
}
