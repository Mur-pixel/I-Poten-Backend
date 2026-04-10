package com.cygnus.iptn.wordbook.service.request;

import com.cygnus.iptn.account.entity.Account;
import com.cygnus.iptn.wordbook.entity.Wordbook;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateWordbookRequest {

    private final Long accountId;
    private final String wordbookName;

    public Wordbook toWordbook(Integer sortOrder, String normalized) {
        return new Wordbook(new Account(accountId), wordbookName, sortOrder, normalized);
    }
}

