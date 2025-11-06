package com.cygnus.ipoten.term.service;


import com.cygnus.ipoten.term.service.request.CreateTermRequest;
import com.cygnus.ipoten.term.service.request.ListTermRequest;
import com.cygnus.ipoten.term.service.request.UpdateTermRequest;
import com.cygnus.ipoten.term.service.response.CreateTermResponse;
import com.cygnus.ipoten.term.service.response.ListTermResponse;
import com.cygnus.ipoten.term.service.response.UpdateTermResponse;
import org.springframework.http.ResponseEntity;

public interface TermService {
    CreateTermResponse register(CreateTermRequest createTermRequest);
    UpdateTermResponse updateTerm(UpdateTermRequest updateTermRequest);
    ResponseEntity<Void> deleteTerm(Long termId);
    ListTermResponse list(ListTermRequest request);
    ListTermResponse searchByTag(String tag, int page, int size);
}
