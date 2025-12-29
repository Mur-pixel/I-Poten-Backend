package com.cygnus.ipoten.wordbook_pdf.controller.export.request_form;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TermsPdfGenerateRequestForm {
    private Long wordbookId;
    private Long userWordbookId;
    private List<Long> termIds;
    private String title;
}
