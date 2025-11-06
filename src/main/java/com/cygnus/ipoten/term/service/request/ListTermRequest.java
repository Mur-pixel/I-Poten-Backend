package com.cygnus.ipoten.term.service.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ListTermRequest {

    private final Integer page;
    private final Integer perPage;
//    private final String query;     // 검색어 활용 시

}
