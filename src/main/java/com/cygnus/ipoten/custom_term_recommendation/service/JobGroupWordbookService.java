package com.cygnus.ipoten.custom_term_recommendation.service;

import com.cygnus.ipoten.wordbook.service.response.AttachTermsBulkResponse;

public interface JobGroupWordbookService {
    AttachTermsBulkResponse attachJobGroupToFolder(Long accountId, Long folderId, String jobKey);
}
