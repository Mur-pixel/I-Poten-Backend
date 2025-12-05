package com.cygnus.ipoten.profileAppearance.Service;

import com.cygnus.ipoten.profileAppearance.Controller.response.AccountSummaryResponse;

public interface AccountSummaryService {
    AccountSummaryResponse getBasicSummary(Long accountId);
}
