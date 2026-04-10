package com.cygnus.iptn.quiz_wrongnote.controller.response_form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WrongNoteListResponseForm {

    private int page;
    private int size;
    private Long total;

    private List<WrongNoteItemResponseForm> items;

    public static WrongNoteListResponseForm of(int page, int size, long total, List<WrongNoteItemResponseForm> items) {
        return WrongNoteListResponseForm.builder()
                .page(page)
                .size(size)
                .total(total)
                .items(items)
                .build();
    }
}
