package com.cygnus.iptn.wordbook.controller.request_form;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MoveWordbookTermsRequestForm {

    @NotNull
    private Long targetWordbookId;

    @NotEmpty
    private List<@NotNull Long> termIds;
}
