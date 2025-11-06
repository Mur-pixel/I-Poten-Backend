package com.cygnus.ipoten.user_term.controller.response_form;

import com.cygnus.ipoten.user_term.service.response.RecordTermViewResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RecordTermViewResponseForm {
    private Long userRecentTermId;
    private Long termId;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private Long viewCount;

    public static RecordTermViewResponseForm from(RecordTermViewResponse response) {
        return new RecordTermViewResponseForm(
                response.getUserRecentTermId(),
                response.getTermId(),
                response.getFirstSeenAt(),
                response.getLastSeenAt(),
                response.getViewCount()
        );
    }
}
