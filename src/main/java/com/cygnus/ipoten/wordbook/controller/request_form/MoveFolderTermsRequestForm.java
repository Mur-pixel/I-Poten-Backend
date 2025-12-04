package com.cygnus.ipoten.wordbook.controller.request_form;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MoveFolderTermsRequestForm {

    @NotNull
    private Long targetFolderId;

    @NotEmpty
    private List<@NotNull Long> termIds;
}
