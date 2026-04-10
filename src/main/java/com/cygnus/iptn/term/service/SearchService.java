package com.cygnus.iptn.term.service;

import com.cygnus.iptn.term.entity.Term;
import com.cygnus.iptn.term.service.request.SearchTermRequest;
import com.cygnus.iptn.term.service.response.SearchTermResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SearchService {
    SearchTermResponse search(SearchTermRequest request);
    List<Long> resolveTargetCategoryIds(SearchTermRequest request);
    Pageable buildPageable(SearchTermRequest request, List<Long> targetCatIds);
    Page<Term> switchPrefix(SearchTermRequest request, Pageable pageable, List<Long> targetCatIds);
    SearchTermResponse toResponse(Page<Term> page);
}
