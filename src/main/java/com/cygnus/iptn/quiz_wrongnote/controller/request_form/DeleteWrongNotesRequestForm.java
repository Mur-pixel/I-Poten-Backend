package com.cygnus.iptn.quiz_wrongnote.controller.request_form;

import lombok.Getter;

import java.util.List;

@Getter
public class DeleteWrongNotesRequestForm {
    private List<Long> reviewIds;
}
