package com.cygnus.iptn.wordbook.service.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ListWordbookTermRequest {

    private final Long accountId;
    private final Long wordbookId;
    private final Integer page;
    private final Integer perPage;
    private final String sort;      // "createdAt, desc" | "title, asc" ...

}
