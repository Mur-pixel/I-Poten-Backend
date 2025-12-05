package com.cygnus.ipoten.wordbook.service.request;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReorderWordbookFoldersRequest {
    private final Long accountId;
    private final List<Long> orderedIds; // 최종 순서대로 정렬된 id 목록
}
