package com.cygnus.iptn.term.service;

import com.cygnus.iptn.term_category.entity.TermCategory;
import com.cygnus.iptn.term.entity.Tag;
import com.cygnus.iptn.term.entity.Term;
import com.cygnus.iptn.term.entity.TermTag;
import com.cygnus.iptn.term_category.repository.TermCategoryRepository;
import com.cygnus.iptn.term.repository.TagRepository;
import com.cygnus.iptn.term.repository.TermRepository;
import com.cygnus.iptn.term.repository.TermTagRepository;
import com.cygnus.iptn.term.service.request.CreateTermRequest;
import com.cygnus.iptn.term.service.request.ListTermRequest;
import com.cygnus.iptn.term.service.request.UpdateTermRequest;
import com.cygnus.iptn.term.service.response.CreateTermResponse;
import com.cygnus.iptn.term.service.response.ListTermResponse;
import com.cygnus.iptn.term.service.response.UpdateTermResponse;
import com.cygnus.iptn.term.support.TagTextParser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TermServiceImpl implements TermService {

    private final TermRepository termRepository;
    private final TermCategoryRepository termCategoryRepository;
    private final TagRepository tagRepository;
    private final TermTagRepository termTagRepository;

    @Override
    @Transactional
    public CreateTermResponse register(CreateTermRequest createTermRequest) {

        // 카테고리 조회
        // 처음 테스트를 수행하는 경우 DB에 categoryId가 등록되어 있지 않아 Null 값 발생 → DataInitializer.java 에서 임시 데이터 넣을 수 있도록 해둠
        TermCategory termCategory = termCategoryRepository.findById(createTermRequest.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        if (!termCategory.getType().equals("언어 중심") && termCategory.getDepth() != 2) {
            throw new IllegalArgumentException("용어 등록은 소분류(또는 언어 중심 중분류)에서만 가능합니다.");
        }

        // 중복 확인
        if (termRepository.existsByTermCategory_IdAndTitle(termCategory.getId(), createTermRequest.getTitle())) {
            log.warn("Duplicate term skipped: categoryId={}, title={}", termCategory.getId(), createTermRequest.getTitle());
            Term existing = termRepository.findByTermCategoryIdAndTitle(termCategory.getId(), createTermRequest.getTitle())
                    .orElseThrow();

            List<String> existingTags = termTagRepository.findAllByTerm(existing).stream()
                    .map(tt -> tt.getTag().getName())
                    .toList();

            // 중복일 때는 duplicate() 사용
            return CreateTermResponse.duplicate(existing, existingTags, termCategory);
        }

        // Term 생성 및 저장
        Term term = createTermRequest.toTerm(termCategory);
        Term savedTerm = termRepository.save(term);

        // 중복 제거 및 정렬
        List<String> tagNames = TagTextParser.parse(createTermRequest.getTags())
                .stream()
                .distinct()
                .toList();

        // 태그 저장 및 TermTag 연결
        List<String> savedTagNames = tagNames.stream().map(tagName -> {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(null, tagName)));
            termTagRepository.save(new TermTag(savedTerm, tag));
            return tag.getName();
        }).toList();

        return CreateTermResponse.from(savedTerm, savedTagNames, termCategory);
    }

    @Override
    @Transactional
    public UpdateTermResponse updateTerm(UpdateTermRequest updateTermRequest) {
        TermCategory termCategory = termCategoryRepository.findById(updateTermRequest.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        if (termCategory.getDepth() != 2) {
            throw new IllegalArgumentException("용어 수정 시 소분류만 선택할 수 있습니다.");
        }

        Term existingTerm = termRepository.findById(updateTermRequest.getTermId())
                .orElseThrow(() -> new IllegalArgumentException("요청하신 용어를 찾을 수 없습니다."));

        existingTerm.setTitle(updateTermRequest.getTitle());
        existingTerm.setDescription(updateTermRequest.getDescription());
        existingTerm.setTermCategory(termCategory);
        Term updatedTerm = termRepository.save(existingTerm);

        // 태그 갱신
        termTagRepository.deleteByTerm(existingTerm);

        // TagTextParser 사용
        List<String> tagNames = TagTextParser.parse(updateTermRequest.getTags())
                .stream()
                .distinct()
                .toList();

        // 태그 저장 및 TermTag 연결
        List<String> updatedTagNames = tagNames.stream().map(tagName -> {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(null, tagName)));
            termTagRepository.save(new TermTag(updatedTerm, tag));
            return tag.getName();
        }).toList();

        return UpdateTermResponse.from(updatedTerm, updatedTagNames, termCategory);
    }

    @Override
    @Transactional
    public ResponseEntity<Void> deleteTerm(Long termId) {

        Term term = termRepository.findById(termId)
                .orElseThrow(() -> new IllegalArgumentException("요청하신 용어를 찾을 수 없습니다."));

        termTagRepository.deleteByTerm(term);
        termRepository.delete(term);
        return ResponseEntity.ok().build();
    }

    @Override
    public ListTermResponse list(ListTermRequest request) {
        PageRequest pageRequest = PageRequest.of(
                request.getPage() - 1,
                request.getPerPage());

        Page<Term> paginated = termRepository.findAll(pageRequest);
        return ListTermResponse.from(paginated);
    }

    @Override
    public ListTermResponse searchByTag(String tag, int page, int size) {

        if(page <1 || size <1) {
            throw new IllegalArgumentException("잘못된 page/size 파라미터입니다.");
        }

        // 태그 정규화 : 소문자, 앞뒤 trim, # 제거
        String normalized = tag == null ? "" : tag.trim().toLowerCase().replaceFirst("^#", "");
        if (normalized.isBlank()) {
            return ListTermResponse.empty();
        }

        PageRequest pageable = PageRequest.of(page - 1, size);
        Page<Term> terms = termRepository.findByTagNameIgnoreCase(normalized, pageable);
        return ListTermResponse.from(terms);
    }

}
