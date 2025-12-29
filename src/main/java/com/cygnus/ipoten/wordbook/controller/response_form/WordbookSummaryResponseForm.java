package com.cygnus.ipoten.wordbook.controller.response_form;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WordbookSummaryResponseForm {
    private Long id;
    private String name;
    private Long termCount;
    private Long learnedCount;
    private Instant updatedAt;
    private Instant lastStudiedAt;
}
