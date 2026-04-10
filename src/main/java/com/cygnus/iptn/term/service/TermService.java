package com.cygnus.iptn.term.service;


import com.cygnus.iptn.term.service.request.CreateTermRequest;
import com.cygnus.iptn.term.service.request.ListTermRequest;
import com.cygnus.iptn.term.service.request.UpdateTermRequest;
import com.cygnus.iptn.term.service.response.CreateTermResponse;
import com.cygnus.iptn.term.service.response.ListTermResponse;
import com.cygnus.iptn.term.service.response.UpdateTermResponse;
import org.springframework.http.ResponseEntity;

public interface TermService {
    CreateTermResponse register(CreateTermRequest createTermRequest);
    UpdateTermResponse updateTerm(UpdateTermRequest updateTermRequest);
    ResponseEntity<Void> deleteTerm(Long termId);
    ListTermResponse list(ListTermRequest request);
    ListTermResponse searchByTag(String tag, int page, int size);
}
