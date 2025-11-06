package com.cygnus.ipoten.user_term.entity.enums;

/**
 * 사용자가 특정 용어를 어떤 경로를 통해 열람했는지 나타내는 Enum
 * '최근 본 용어(UserRecentTerm)' 기록 시, 소스 정보를 저장하여 추후 통계, 추천, 사용자 행동 분석 등에 활용 가능
 */

public enum ViewSource {
    DETAIL, // 용어 상세 화면(Detail Page)에서 직접 조회한 경우
    SEARCH, // 검색 결과(Search Result)에서 용어를 클릭하여 조회한 경우
    QUIZ, // 퀴즈 풀이 후 해설 보기에서 해당 용어를 열람한 경우
    SHARE // 공유된 링크(Share Link)를 통해 외부 또는 내부에서 용어 페이지에 진입한 경우
}
