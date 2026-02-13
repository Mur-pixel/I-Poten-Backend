package com.cygnus.ipoten.ebook.service;

import com.cygnus.ipoten.ebook.entity.Ebook;
import com.cygnus.ipoten.ebook.repository.EbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EbookServiceImpl implements EbookService {

    private final EbookRepository ebookRepository;

    @Override
    public List<Ebook> listAll() {
        return ebookRepository.findAll().stream()
                .sorted(Comparator.comparing(Ebook::getCreatedAt).reversed()).toList();
    }
}
