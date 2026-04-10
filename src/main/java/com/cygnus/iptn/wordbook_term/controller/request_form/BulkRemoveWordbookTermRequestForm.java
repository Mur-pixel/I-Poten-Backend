package com.cygnus.iptn.wordbook_term.controller.request_form;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class BulkRemoveWordbookTermRequestForm {

    @NotEmpty(message = "termIds는 비어 있을 수 없습니다.")
    private List<Long> termIds;
}
