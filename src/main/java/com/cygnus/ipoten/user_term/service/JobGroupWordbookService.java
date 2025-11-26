package com.cygnus.ipoten.user_term.service;

import com.cygnus.ipoten.user_term.service.response.AttachTermsBulkResponse;

public interface JobGroupWordbookService {
    AttachTermsBulkResponse attachJobGroupToFolder(Long accountId, Long folderId, String jobKey);
}
