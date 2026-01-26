package com.cygnus.ipoten.credit.event;

public class AccountSignedUpEvent {
    private final Long accountId;

    public AccountSignedUpEvent(Long accountId) {
        this.accountId = accountId;
    }

    public Long getAccountId() {
        return accountId;
    }
}
