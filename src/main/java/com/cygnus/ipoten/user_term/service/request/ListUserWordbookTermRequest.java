package com.cygnus.ipoten.user_term.service.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ListUserWordbookTermRequest {

    private final Long accountId;
    private final Long folderId;
    private final Integer page;     // 1-based 입력
    private final Integer perPage;
    private final String sort;      // "createdAt, desc" | "title, asc" ...

}
