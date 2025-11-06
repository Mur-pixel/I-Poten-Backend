package com.cygnus.ipoten.user_term.service.request;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.user_term.entity.UserWordbookFolder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateUserWordbookFolderRequest {

    private final Long accountId;
    private final String folderName;

    public UserWordbookFolder toUserWordbookFolder(Integer sortOrder) {
        Account account = new Account(accountId);
        return new UserWordbookFolder(account, folderName, sortOrder);
    }

    public UserWordbookFolder toUserWordbookFolder(Integer sortOrder, String normalized) {
        return new UserWordbookFolder(new Account(accountId), folderName, sortOrder, normalized);
    }
}

