package com.cygnus.ipoten.wordbook.controller.response_form;

import com.cygnus.ipoten.wordbook.service.response.RenameWordbookResponse;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RenameWordbookResponseForm {
    Long id;
    String wordbookName;
    Integer sortOrder;
    String createdAtKst;
    String updatedAtKst;

    public static RenameWordbookResponseForm from(RenameWordbookResponse r) {
        return RenameWordbookResponseForm.builder()
                .id(r.getId())
                .wordbookName(r.getWordbookName())
                .sortOrder(r.getSortOrder())
                .createdAtKst(r.getCreatedAtKst())
                .updatedAtKst(r.getUpdatedAtKst())
                .build();
    }
}
