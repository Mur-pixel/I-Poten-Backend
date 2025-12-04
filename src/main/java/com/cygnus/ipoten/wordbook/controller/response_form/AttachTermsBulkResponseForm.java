package com.cygnus.ipoten.wordbook.controller.response_form;

import com.cygnus.ipoten.wordbook.service.response.AttachTermsBulkResponse;

import java.util.List;

public record AttachTermsBulkResponseForm(
        Long folderId,
        int requested,
        int attached,
        int skipped,
        int failed,
        List<Long> invalidIds,
        String message
) {
    public static AttachTermsBulkResponseForm from(AttachTermsBulkResponse response) {
        return new AttachTermsBulkResponseForm(
                response.folderId(),
                response.requested(),
                response.attached(),
                response.skipped(),
                response.failed(),
                response.invalidIds(),
                "단어 일괄 추가가 완료되었습니다."
        );
    }
}
