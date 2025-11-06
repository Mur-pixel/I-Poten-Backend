package com.cygnus.ipoten.interview.entity;

/**
 * 한글 회사 이름을 ChromaDB에서 사용 가능한 영문 이름으로 매핑하는 Enum
 * ChromaDB는 컬렉션 이름에 영문자, 숫자, 점(.), 밑줄(_), 하이픈(-)만 허용함
 */
public enum CompanyNameMapping {
    // ChromaDB에 실제로 저장된 회사들
    KARROT("당근마켓", "karrot"),
    DANGGEUN("당근", "danggeun"),
    TOSS("토스", "toss"),
    KT_MOBILE("KT모바일", "kt_mobile"),
    SK_ENCORE("SK엔코아", "sk_encore"),

    // 추가 회사들 (필요시 ChromaDB에 데이터 추가 필요)
    NAVER("네이버", "naver"),
    KAKAO("카카오", "kakao"),
    COUPANG("쿠팡", "coupang"),
    LINE("라인", "line"),
    WOOWAHAN("우아한형제들", "woowahan"),
    BAEMIN("배달의민족", "baemin"),
    YANOLJA("야놀자", "yanolja"),
    KURLY("컬리", "kurly"),
    ZIGBANG("직방", "zigbang"),
    MUSINSA("무신사", "musinsa"),
    WATCHA("왓챠", "watcha"),
    SAMSUNG("삼성", "samsung"),
    LG("엘지", "lg"),
    SK("에스케이", "sk"),
    HYUNDAI("현대", "hyundai"),
    LOTTE("롯데", "lotte"),
    KT("케이티", "kt"),
    // 필요한 회사 추가
    UNKNOWN("", "unknown");

    private final String koreanName;
    private final String englishName;

    CompanyNameMapping(String koreanName, String englishName) {
        this.koreanName = koreanName;
        this.englishName = englishName;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public String getEnglishName() {
        return englishName;
    }

    /**
     * 한글 회사 이름을 영문 이름으로 변환
     * @param koreanName 한글 회사 이름
     * @return 매핑된 영문 이름 또는 기본값
     */
    public static String toEnglishName(String koreanName) {
        if (koreanName == null) return UNKNOWN.getEnglishName();

        for (CompanyNameMapping mapping : values()) {
            if (mapping.getKoreanName().equals(koreanName)) {
                return mapping.getEnglishName();
            }
        }
        
        // 매핑이 없는 경우 알파벳과 숫자만 추출
        String sanitized = koreanName.replaceAll("[^a-zA-Z0-9]", "");
        if (!sanitized.isEmpty()) {
            return sanitized.toLowerCase();
        }
        
        // 알파벳과 숫자가 없는 경우 기본값 반환
        return "company" + Math.abs(koreanName.hashCode());
    }
}
