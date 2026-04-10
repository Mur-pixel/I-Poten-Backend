package com.cygnus.ipoten.wordbook_pdf.service.export.response;

import com.cygnus.ipoten.wordbook_pdf.service.export.PdfRenderer;
import com.cygnus.ipoten.wordbook_pdf.service.export.PdfGenerateRequest;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term.repository.TermRepository;
import com.cygnus.ipoten.term.repository.TermTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final TermRepository termRepository;
    private final TermTagRepository termTagRepository;
    private final PdfRenderer pdfRenderer;

    @Transactional(readOnly = true)
    public PdfGenerateResult generate(PdfGenerateRequest request) {
        // 1) 검증
        List<Long> ids = Optional.ofNullable(request.getTermIds())
                .filter(list -> !list.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException("termIds는 비어 있을 수 없습니다."));

        // 2) 용어 조회
        List<Term> terms = termRepository.findAllById(ids);
        if (terms.size() != ids.size()) {
            throw new NoSuchElementException("요청한 용어 ID 중 존재하지 않는 항목이 있습니다.");
        }

        // 3) term_id -> "tag1, tag2, ..." 맵 구성 (조인 테이블에서 바로 집계)
        var rows = termTagRepository.findTermIdAndTagNameByTermIdIn(ids);

        Map<Long, String> tagsCsvByTermId = rows.stream()
                .filter(r -> r.getTagName() != null && !r.getTagName().isBlank())
                .collect(Collectors.groupingBy(
                        TermTagRepository.Row::getTermId,
                        Collectors.mapping(TermTagRepository.Row::getTagName,
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(() -> new java.util.TreeSet<>(String::compareTo)),
                                        set -> String.join(", ", set)
                                )
                        )
                ));

        // 4) 요청 순서 보존 + View 변환
        Map<Long, Term> byId = terms.stream().collect(Collectors.toMap(Term::getId, Function.identity()));

        // (A) PdfRenderer.render가 List<? extends TermView>를 받는 경우: 그대로 OK
        var ordered = ids.stream().map(id -> {
            Term t = byId.get(id);
            String tagsCsv = tagsCsvByTermId.getOrDefault(id, "");
            return new PdfRenderer.TermView() {
                public Long getId() { return t.getId(); }
                public String getTerm() { return t.getTitle(); }
                public String getDescription() { return t.getDescription(); }
                public String getTags() { return tagsCsv; }
            };
        }).toList();

        // 5) 렌더
        String title = (request.getTitle() == null || request.getTitle().isBlank())
                ? "내 단어장 PDF" : request.getTitle();
        String filename = "I-Ptn_terms_" + LocalDate.now() + ".pdf";

        PdfStream stream = out -> pdfRenderer.render(title, ordered, out);

        PdfGenerateResponse meta = PdfGenerateResponse.builder()
                .wordbookPdfId(null)
                .filename(filename)
                .count(ordered.size())
                .build();

        return new PdfGenerateResult(meta, stream);
    }

    public record PdfGenerateResult(PdfGenerateResponse meta, PdfStream stream) {}
}
