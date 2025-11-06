package com.cygnus.ipoten.user_term.controller.request_form;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkDeleteFoldersRequestForm {
    @NotEmpty(message = "folderIds가 필요합니다.")
    private List<Long> folderIds;
}
