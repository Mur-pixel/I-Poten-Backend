package com.cygnus.iptn.wordbook.controller.request_form;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkDeleteWordbookRequestForm {
    @NotEmpty(message = "wordbookIds 필요합니다.")
    private List<Long> wordbookIds;
}
