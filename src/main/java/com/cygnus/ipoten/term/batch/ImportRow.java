package com.cygnus.ipoten.term.batch;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImportRow {
    private Long categoryId;
    private String termId;          //  UUID 문자열
    private String title;
    private String description;
    private String tags;
}
