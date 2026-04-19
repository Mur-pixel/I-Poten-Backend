package com.cygnus.ipoten.interview.controller.response_form;

import java.util.List;

public record AdminInterviewUsersResponseForm(
        List<AdminInterviewUserItemResponseForm> items,
        int pageSize,
        boolean hasNext,
        Long nextCursor
) {
}
