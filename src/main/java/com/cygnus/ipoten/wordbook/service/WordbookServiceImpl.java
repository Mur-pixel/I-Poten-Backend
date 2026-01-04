package com.cygnus.ipoten.wordbook.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.repository.AccountRepository;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term.repository.TermRepository;
import com.cygnus.ipoten.term.repository.TermTagRepository;
import com.cygnus.ipoten.wordbook.entity.Wordbook;
import com.cygnus.ipoten.wordbook_term.entity.WordbookTerm;
import com.cygnus.ipoten.wordbook_learning.repository.LearningProgressRepository;
import com.cygnus.ipoten.wordbook.repository.WordbookRepository;
import com.cygnus.ipoten.wordbook_term.repository.WordbookTermRepository;
import com.cygnus.ipoten.wordbook.service.request.*;
import com.cygnus.ipoten.wordbook.service.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordbookServiceImpl implements WordbookService {

    private final WordbookRepository wordbookRepository;
    private final WordbookTermRepository wordbookTermRepository;
    private final AccountRepository accountRepository;
    private final TermRepository termRepository;
    private final LearningProgressRepository learningProgressRepository;
    private final TermTagRepository termTagRepository;

    @Value("${ebook.max.termids.per.wordbook:5000}")
    private int maxTermIdsPerFolder;

    @Override
    @Transactional
    public CreateWordbookResponse registerWordbook(CreateWordbookRequest request) {
        Long accountId = request.getAccountId();

        String raw = request.getWordbookName();
        String normalized = normalize(raw).toLowerCase();
        if (normalized.isBlank())
            throw new ResponseStatusException(BAD_REQUEST, "폴더명을 입력해 주세요.");

        if (wordbookRepository.existsByAccount_IdAndNormalizedWordbookNameAndIdNot(accountId, normalized, null))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 폴더명입니다.");

        int nextOrder = wordbookRepository.findMaxSortOrderByAccountId(accountId) + 1;
        var entity = request.toWordbook(nextOrder, normalized);

        try {
            var saved = wordbookRepository.save(entity);
            return CreateWordbookResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 폴더명입니다.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ListWordbookTermResponse list(ListWordbookTermRequest request) {
        Long accountId = request.getAccountId();
        Long wordbookId = request.getWordbookId();

        boolean owns = wordbookRepository.existsByIdAndAccount_Id(wordbookId, accountId);
        log.info("[list] owns? accountId={}, wordbookId={}, result={}", accountId, wordbookId, owns);
        if (!owns) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 폴더를 찾을 수 없습니다.");

        int pageIdx = Math.max(0, request.getPage());
        Integer per = request.getPerPage();
        int size = (per == null) ? 20 : Math.min(Math.max(5, per), 100);
        Sort sort   = parseSortOrDefault(request.getSort(), Sort.by(Sort.Order.desc("createdAt")));
        Pageable pageable = PageRequest.of(pageIdx, size, sort);

        log.info("[list] call repo: wordbookId={}, accountId={}, pageable={}", wordbookId, accountId, pageable);

        Page<WordbookTerm> paginatedList =
                wordbookTermRepository.findPageByFolderAndOwnerFetch(wordbookId, accountId, pageable);

        return ListWordbookTermResponse.from(paginatedList);
    }

    /** 폴더 전체 재정렬 */
    @Override
    @Transactional
    public void reorder(ReorderWordbookRequest request) {
        final Long accountId = request.getAccountId();
        final List<Long> orderedIds = Optional.ofNullable(request.getOrderedIds())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "ids가 필요합니다."));

        final Set<Long> dedup = new LinkedHashSet<>(orderedIds);
        if (dedup.size() != orderedIds.size())
            throw new ResponseStatusException(BAD_REQUEST, "ids에 중복이 포함되어 있습니다.");

        final List<Wordbook> all =
                wordbookRepository.findAllByAccount_IdOrderBySortOrderAscIdAsc(accountId);
        if (all.isEmpty() && !orderedIds.isEmpty())
            throw new ResponseStatusException(BAD_REQUEST, "정렬할 폴더가 존재하지 않습니다.");

        final Set<Long> allIds = all.stream().map(Wordbook::getId).collect(Collectors.toSet());
        if (!allIds.equals(dedup))
            throw new ResponseStatusException(BAD_REQUEST, "ids가 계정의 폴더 전체 집합과 일치하지 않습니다.");

        int idx = 0;
        Map<Long, Integer> toOrder = new HashMap<>();
        for (Long id : orderedIds) toOrder.put(id, idx++);

        for (Wordbook wordbook : all) {
            Integer newOrder = toOrder.get(wordbook.getId());
            if (newOrder == null) throw new ResponseStatusException(NOT_FOUND, "요청하신 폴더를 찾을 수 없습니다.");
            if (!Objects.equals(wordbook.getSortOrder(), newOrder)) {
                wordbook.setSortOrder(newOrder);
            }
        }
        wordbookRepository.saveAll(all);
    }

    @Override
    @Transactional
    public CreateWordbookTermResponse attachTerm(CreateWordbookTermRequest request) {
        Long accountId = request.getAccountId();
        Long wordbookId = request.getWordbookId();
        Long termId = request.getTermId();

        if (accountId == null || wordbookId == null || termId == null || accountId <= 0 || wordbookId <= 0 || termId <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "잘못된 파라미터입니다.");
        }

        boolean owns = wordbookRepository.existsByIdAndAccount_Id(wordbookId, accountId);
        if (!owns) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "폴더에 대한 권한이 없습니다.");

        Optional<WordbookTerm> existing =
                wordbookTermRepository.findByAccount_IdAndWordbook_IdAndTerm_Id(accountId, wordbookId, termId);

        if (existing.isPresent()) {
            return CreateWordbookTermResponse.alreadyAttached(existing.get().getId(), wordbookId, termId);
        }

        if (!termRepository.existsById(termId)) {
            throw new ResponseStatusException(NOT_FOUND, "용어를 찾을 수 없습니다.");
        }

        Wordbook wordbookRef = wordbookRepository.getReferenceById(wordbookId);
        Account accountRef = accountRepository.getReferenceById(accountId);
        Term termRef = termRepository.getReferenceById(termId);

        WordbookTerm uwt = new WordbookTerm(accountRef, wordbookRef, termRef);
        WordbookTerm saved = wordbookTermRepository.save(uwt);

        return CreateWordbookTermResponse.created(saved.getId(), wordbookId, termId);
    }

    @Override
    @Transactional
    public RenameWordbookResponse rename(RenameWordbookRequest req) {
        final Long accountId = req.getAccountId();
        final Long wordbookId  = req.getWordbookId();

        Wordbook wordbook = wordbookRepository.findById(wordbookId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "폴더를 찾을 수 없습니다."));

        if (!wordbook.getAccount().getId().equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        String raw = req.getWordbookName();
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "wordbookName은 공백일 수 없습니다.");
        }

        String normalized = normalizeLikeEntity(raw);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "wordbookName은 공백일 수 없습니다.");
        }
        if (normalized.length() > 50) {
            throw new ResponseStatusException(BAD_REQUEST, "wordbookName은 최대 50자입니다.");
        }

        // 변경 없음
        if (normalized.equals(wordbook.getNormalizedWordbookName())) {
            return map(wordbook);
        }

        // 중복 체크 (자기 자신 제외)
        boolean dup = wordbookRepository
                .existsByAccount_IdAndNormalizedWordbookNameAndIdNot(accountId, normalized, wordbookId);
        if (dup) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "동일한 이름의 폴더가 이미 존재합니다.");
        }

        wordbook.setWordbookName(raw);
        wordbookRepository.save(wordbook); // @PreUpdate에서 updatedAt 갱신됨

        return map(wordbook);
    }

    @Override
    @Transactional
    public void deleteOne(Long accountId, DeleteMode mode, Long wordbookId, Long targetWordbookId) {
        var wordbook = wordbookRepository.findByIdAndAccount_Id(wordbookId, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "폴더를 찾을 수 없습니다."));

        long count = wordbookTermRepository.countByWordbookIdAndAccountId(wordbookId, accountId);

        switch (mode) {
            case FORBID -> {
                if (count > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "폴더에 항목이 있어 삭제할 수 없습니다.");
            }
            case DETACH -> {
                if (count > 0) wordbookTermRepository.deleteByWordbookIdAndAccountId(wordbookId, accountId);
            }
            case MOVE -> {
                if (targetWordbookId == null) throw new ResponseStatusException(BAD_REQUEST, "targetWordbookId가 필요합니다.");
                if (Objects.equals(targetWordbookId, wordbookId))
                    throw new ResponseStatusException(BAD_REQUEST, "targetWordbookId가 삭제 대상과 같습니다.");
                wordbookRepository.findByIdAndAccount_Id(targetWordbookId, accountId)
                        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "대상 폴더를 찾을 수 없습니다."));
                if (count > 0) wordbookTermRepository.bulkUpdateMoveFolder(wordbookId, targetWordbookId, accountId);
            }
            case PURGE -> {
                if (count > 0) wordbookTermRepository.deleteByWordbookIdAndAccountId(wordbookId, accountId);
            }
        }

        wordbookRepository.delete(wordbook);
        resequenceSortOrder(accountId);
    }

    @Override
    @Transactional
    public void deleteBulk(Long accountId, DeleteMode mode, List<Long> wordbookIds, Long targetWordbookId) {
        if (wordbookIds == null || wordbookIds.isEmpty()) return;

        long owned = wordbookRepository.countOwnedByIds(accountId, wordbookIds);
        if (owned != wordbookIds.size())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "소유하지 않은 폴더가 포함되어 있습니다.");

        if (mode == DeleteMode.MOVE) {
            if (targetWordbookId == null)
                throw new ResponseStatusException(BAD_REQUEST, "targetWordbookId가 필요합니다.");
            wordbookRepository.findByIdAndAccount_Id(targetWordbookId, accountId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "대상 폴더를 찾을 수 없습니다."));
            if (wordbookIds.contains(targetWordbookId))
                throw new ResponseStatusException(BAD_REQUEST, "targetWordbookId는 삭제 대상에 포함될 수 없습니다.");
        }

        if (mode == DeleteMode.FORBID) {
            for (Long fid : wordbookIds) {
                long c = wordbookTermRepository.countByWordbookIdAndAccountId(fid, accountId);
                if (c > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "항목이 있는 폴더가 포함되어 있어 삭제할 수 없습니다.");
            }
        } else if (mode == DeleteMode.DETACH) {
            for (Long fid : wordbookIds) {
                wordbookTermRepository.deleteByAccountIdAndWordbookIdIn(accountId, wordbookIds);
            }
        } else if (mode == DeleteMode.MOVE) {
            for (Long fid : wordbookIds) {
                wordbookTermRepository.bulkUpdateMoveFolder(fid, targetWordbookId, accountId);
            }
        } else if (mode == DeleteMode.PURGE) {
            wordbookTermRepository.deleteByAccountIdAndWordbookIdIn(accountId, wordbookIds);
        }

        wordbookRepository.deleteByAccount_IdAndIdIn(accountId, wordbookIds);
        resequenceSortOrder(accountId);
    }

    @Override
    public TermIdsResult getAllTermIds(Long accountId, Long wordbookId) {
        // 소유권 검증
        boolean owns = wordbookRepository.existsByIdAndAccount_Id(wordbookId, accountId);
        if (!owns) throw new ResponseStatusException(NOT_FOUND, "폴더를 찾을 수 없습니다.");

        // 단일 패스 조회(중복 제거 + 정렬)
        List<Long> ids = wordbookTermRepository
                .findDistinctTermIdsByWordbookAndAccountOrderByTermIdAsc(wordbookId, accountId);

        int total = ids.size();
        boolean limitExceeded = total > maxTermIdsPerFolder;

        // 상한 초과 시 본문 termIds는 비워서 전송
        return new TermIdsResult(wordbookId, limitExceeded ? List.of() : ids, limitExceeded, maxTermIdsPerFolder, total);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportTermIdsResult collectExportTermIds(Long accountId, Long wordbookId, String memorization, List<String> includeTags, List<String> excludeTags, String sort, int hardLimit) {
        // 소유권 검증
        boolean owns = wordbookRepository.existsByIdAndAccount_Id(wordbookId, accountId);
        if (!owns) throw new ResponseStatusException(NOT_FOUND, "폴더를 찾을 수 없습니다.");

        // 폴더 내 termId 전체 (중복 제거 + 정렬)
        List<Long> base = wordbookTermRepository
                .findDistinctTermIdsByWordbookAndAccountOrderByTermIdAsc(wordbookId, accountId);

        final int totalBefore = base.size();
        if (totalBefore == 0) {
            return new ExportTermIdsResult(wordbookId, List.of(), 0, 0, false, hardLimit, 0);
        }

        // 필터 암기 상태
        List<Long> filtered = new ArrayList<>(base);
        if (memorization != null && !memorization.isBlank()) {
            var rows = learningProgressRepository.findByIdAccountIdAndIdTermIdIn(accountId, filtered);
            // 기본 LEARNING으로 보고 시작 -> DONE만 남기거나 반대로 필터
            Map<Long, String> statusMap = new HashMap<>();
            // 기본값 LEARNING
            for (Long id : filtered) statusMap.put(id, "LEARNING");
            // 저장된 값 덮어쓰기
            rows.forEach(p -> statusMap.put(p.getId().getTermId(), p.getStatus().name()));

            final String want = memorization.toUpperCase(Locale.ROOT);
            filtered = filtered.stream()
                    .filter(id -> Objects.equals(statusMap.get(id), want))
                    .collect(Collectors.toList());
        }

        // 필터 : 태그 포함/제외 (둘 다 지정 시: include 먼저 적용 후 exclude)
        if (includeTags != null && !includeTags.isEmpty()) {
            var rows = termTagRepository.findTermIdAndTagNameByTermIdIn(filtered);
            Map<Long, Set<String>> byTerm = new HashMap<>();
            rows.forEach(r -> byTerm.computeIfAbsent(r.getTermId(), k-> new HashSet<>()).add(r.getTagName()));
            filtered = filtered.stream()
                    .filter(id -> {
                        Set<String> have = byTerm.getOrDefault(id, Set.of());
                        for (String tag : includeTags) {
                            if (!have.contains(tag)) return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }
        if (excludeTags != null && !excludeTags.isEmpty()) {
            var rows = termTagRepository.findTermIdAndTagNameByTermIdIn(filtered);
            Map<Long, Set<String>> byTerm = new HashMap<>();
            rows.forEach(r -> byTerm.computeIfAbsent(r.getTermId(), k-> new HashSet<>()).add(r.getTagName()));
            filtered = filtered.stream()
                    .filter(id ->{
                        Set<String> have = byTerm.getOrDefault(id, Set.of());
                        for (String tag : excludeTags) {
                            if (have.contains(tag)) return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        // 정렬 : 일단 termId 기준만 지원(ASC/DESC)
        if (sort != null && !sort.isBlank()) {
            String[] p = sort.split(",", 2);
            boolean desc = p.length > 1 && "DESC".equalsIgnoreCase(p[1]);
            filtered.sort(desc ? Comparator.<Long>naturalOrder().reversed() : Comparator.naturalOrder());
        }

        int filteredOut = totalBefore - filtered.size();

        // 상한
        int limit = hardLimit > 0 ? hardLimit : maxTermIdsPerFolder;
        boolean exceeded = filtered.size() >= limit;
        List<Long> finalIds = exceeded ? List.of() : filtered;

        return new ExportTermIdsResult(
                wordbookId,
                finalIds,
                totalBefore,
                filteredOut,
                exceeded,
                limit,
                finalIds.size()
        );
    }

    @Override
    @Transactional
    public AttachTermsBulkResponse attachTermsBulk(AttachTermsBulkRequest request) {
        final int MAX_BULK = 2000;
        final int BATCH_SIZE = 500;

        Long accountId = request.accountId();
        Long wordbookId = request.wordbookId();
        List<Long> input = (request.termIds() == null) ? List.of() : request.termIds();

        if (input.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "용어 ID 목록이 비어있습니다.");
        }
        if (input.size() > MAX_BULK) {
            throw new ResponseStatusException(BAD_REQUEST, "용어 ID 개수가 최대 허용 개수(" + MAX_BULK + ")를 초과했습니다.");
        }

        // 폴더 소유권 검증
        Wordbook wordbook = wordbookRepository.findById(wordbookId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "폴더를 찾을 수 없습니다."));
        if (!Objects.equals(wordbook.getAccount().getId(), accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        // 입력 정규화 (null 제거 + 중복 제거)
        List<Long> requestedDistinct = input.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 기존 termId 조회(중복 스킵)
        Set<Long> already = new HashSet<>(
                wordbookTermRepository.findDistinctTermIdsByWordbookAndAccountOrderByTermIdAsc(wordbookId, accountId)
        );
        List<Long> duplicates = requestedDistinct.stream()
                .filter(already::contains)
                .toList();

        // 삽입 후보
        List<Long> candidates = requestedDistinct.stream()
                .filter(id -> !already.contains(id))
                .toList();

        // 존재하는 term만 추리기
        var existingTerms = termRepository.findAllById(candidates);
        Set<Long> existingIds = existingTerms.stream().map(Term::getId).collect(Collectors.toSet());

        List<Long> invalidIds = candidates.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();

        List<Long> toInsertIds = candidates.stream()
                .filter(existingIds::contains)
                .toList();

        // sortOrder 계산
        Integer base = wordbookTermRepository.findMaxSortOrderByAccountAndFolder(accountId, wordbookId);
        int cursor = (base == null ? 0 : base);

        int attached = 0;
        List<WordbookTerm> buffer = new ArrayList<>(Math.min(toInsertIds.size(), BATCH_SIZE));

        for (Long termId : toInsertIds) {
            Term termRef = termRepository.getReferenceById(termId);

            // ✅ account는 wordbook에서 자동으로 가져오게 (이미 WordbookTerm.of가 있음)
            WordbookTerm uwt = WordbookTerm.of(wordbook, termRef, ++cursor);

            buffer.add(uwt);

            if (buffer.size() >= BATCH_SIZE) {
                wordbookTermRepository.saveAll(buffer);
                attached += buffer.size();
                buffer.clear();
            }
        }

        if (!buffer.isEmpty()) {
            wordbookTermRepository.saveAll(buffer);
            attached += buffer.size();
            buffer.clear();
        }

        int requested = input.size();
        int skipped = duplicates.size();
        int failed = 0;

        log.info("[attachTermsBulk] accountId={} wordbookId={} requested={} attached={} skipped={} invalid={}",
                accountId, wordbookId, requested, attached, skipped, invalidIds.size());

        return new AttachTermsBulkResponse(
                wordbookId,
                requested,
                attached,
                skipped,
                failed,
                invalidIds
        );
    }

    /** 엔티티와 동일: trim → 연속 공백 1칸 → lower(Locale.ROOT) */
    private String normalizeLikeEntity(String s) {
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private RenameWordbookResponse map(Wordbook w) {
        return RenameWordbookResponse.builder()
                .id(w.getId())
                .wordbookName(w.getWordbookName())
                .sortOrder(w.getSortOrder())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }

    /* ------------------------------------- */

    private Sort parseSortOrDefault(String raw, Sort fallback) {
        String s = (raw == null || raw.isBlank()) ? "createdAt,desc" : raw.trim();
        String[] parts = s.split(",");
        String key = parts[0].trim();
        String dir = (parts.length > 1 ? parts[1].trim() : "desc");

        // 웹 키 → JPA 경로 매핑
        if ("title".equalsIgnoreCase(key)) key = "term.title";
        else if (!"createdAt".equalsIgnoreCase(key)) key = "createdAt";

        return "desc".equalsIgnoreCase(dir) ? Sort.by(key).descending() : Sort.by(key).ascending();
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    @Transactional
    protected void resequenceSortOrder(Long accountId) {
        // 현재 계정의 폴더를 정렬순/ID순으로 불러와
        List<Wordbook> wordbooks =
                wordbookRepository.findAllByAccount_IdOrderBySortOrderAscIdAsc(accountId);

        // 0부터 증가하는 연속 정수로 sortOrder 재부여
        int i = 0;
        for (Wordbook f : wordbooks) {
            if (f.getSortOrder() == null || !Objects.equals(f.getSortOrder(), i)) {
                f.setSortOrder(i);
            }
            i++;
        }

        // 변경사항 저장
        wordbookRepository.saveAll(wordbooks);
    }
}
