package com.cygnus.ipoten.wordbook_term.service;

import com.cygnus.ipoten.wordbook.service.response.MoveWordbookTermsResponse;

import java.util.List;

public interface WordbookTermService {
    MoveWordbookTermsResponse moveTerms(Long accountId, Long sourceWordbookId, Long targetWordbookId, List<Long> termIds);
    void removeTermsFromWordbook(Long accountId, Long wordbookId, List<Long> termIds);
}
