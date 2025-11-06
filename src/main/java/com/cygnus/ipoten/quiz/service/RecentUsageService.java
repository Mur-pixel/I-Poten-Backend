package com.cygnus.ipoten.quiz.service;

import java.util.Set;

public interface RecentUsageService {

    /** 최근 N일 내에 사용자가 본(제출한) 문항의 TermId 집합 */
    Set<Long> findRecentTermIds(Long accountId, int lastNDays);

    /** 최근 N일 내에 사용자가 본 보기(choiceText)의 '정규화 텍스트' 집합 */
    Set<String> findRecentChoiceNorms(Long accountId, int lastNDays);
}
