package com.cygnus.iptn.quiz_wrongnote.controller.request_form;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WrongNoteResolvedUpdateRequestForm {
    private Boolean resolved;   // true면 RESOLVED, false면 UNRESOLVED
}