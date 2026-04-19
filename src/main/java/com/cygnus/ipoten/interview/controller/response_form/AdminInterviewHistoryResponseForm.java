package com.cygnus.ipoten.interview.controller.response_form;

import java.util.List;

public record AdminInterviewHistoryResponseForm(
        AdminInterviewOwnerResponseForm user,
        AdminInterviewSummaryResponseForm summary,
        List<AdminInterviewHistoryItemResponseForm> items,
        int pageSize,
        boolean hasNext,
        Long nextCursor
) {
}
