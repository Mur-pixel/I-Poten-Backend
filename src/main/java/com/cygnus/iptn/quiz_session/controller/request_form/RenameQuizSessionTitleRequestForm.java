package com.cygnus.iptn.quiz_session.controller.request_form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RenameQuizSessionTitleRequestForm {

    @NotBlank(message = "title은 비어 있을 수 없습니다.")
    @Size(min = 1, max = 60, message = "title은 1~60자여야 합니다.")
    private String title;
}
