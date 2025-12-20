package com.cygnus.ipoten.wordbook.controller.response_form;

import com.cygnus.ipoten.wordbook.service.response.RenameWordbookResponse;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class RenameWordbookResponseForm {
    Long id;
    String wordbookName;
    Integer sortOrder;
    Instant updatedAt;

    public static RenameWordbookResponseForm from(RenameWordbookResponse r) {
        return RenameWordbookResponseForm.builder()
                .id(r.getId())
                .wordbookName(r.getWordbookName())
                .sortOrder(r.getSortOrder())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
