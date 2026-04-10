package com.cygnus.iptn.term_trending.controller.request_form;

import com.cygnus.iptn.term_trending.service.request.TrendingTermRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrendingTermRequestForm {

    @NotBlank
    private String range = "7d";

    @Min(1)
    @Max(100)
    private int limit = 20;

    public TrendingTermRequest toRequest() {
        return new TrendingTermRequest(range, limit);
    }
}
