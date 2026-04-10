package com.cygnus.iptn.wordbook_term.service;

import com.cygnus.iptn.wordbook.service.response.MoveWordbookTermsResponse;

import java.util.List;

public interface WordbookTermService {
    MoveWordbookTermsResponse moveTerms(Long accountId, Long sourceWordbookId, Long targetWordbookId, List<Long> termIds);
    void removeTermsFromWordbook(Long accountId, Long wordbookId, List<Long> termIds);
}
