package com.cygnus.iptn.credit.event;

public class AccountSignedUpEvent {
    private final Long accountId;

    public AccountSignedUpEvent(Long accountId) {
        this.accountId = accountId;
    }

    public Long getAccountId() {
        return accountId;
    }
}
