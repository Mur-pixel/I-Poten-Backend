package com.cygnus.ipoten.wordbook.service.request;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.wordbook.entity.WordbookFolder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateWordbookFolderRequest {

    private final Long accountId;
    private final String folderName;

    public WordbookFolder toWordbookFolder(Integer sortOrder, String normalized) {
        return new WordbookFolder(new Account(accountId), folderName, sortOrder, normalized);
    }
}

