package com.cygnus.ipoten.wordbook.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.repository.AccountRepository;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term.repository.TermRepository;
import com.cygnus.ipoten.term.repository.TermTagRepository;
import com.cygnus.ipoten.wordbook.entity.WordbookFolder;
import com.cygnus.ipoten.wordbook.entity.WordbookTerm;
import com.cygnus.ipoten.custom_term_learning.repository.LearningProgressRepository;
import com.cygnus.ipoten.wordbook.repository.WordbookFolderRepository;
import com.cygnus.ipoten.wordbook.repository.WordbookTermRepository;
import com.cygnus.ipoten.wordbook.service.request.*;
import com.cygnus.ipoten.wordbook.service.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
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
public class WordbookFolderServiceImpl implements WordbookFolderService {

    private final WordbookFolderRepository wordbookFolderRepository;
    private final WordbookTermRepository wordbookTermRepository;
    private final AccountRepository accountRepository;
    private final TermRepository termRepository;
    private final LearningProgressRepository learningProgressRepository;
    private final TermTagRepository termTagRepository;

    @Value("${ebook.max.termids.per.folder:5000}")
    private int maxTermIdsPerFolder;

    @Override
    @Transactional
    public CreateWordbookFolderResponse registerWordbookFolder(CreateWordbookFolderRequest request) {
        Long accountId = request.getAccountId();

        String raw = request.getFolderName();
        String normalized = normalize(raw).toLowerCase();
        if (normalized.isBlank())
            throw new ResponseStatusException(BAD_REQUEST, "폴더명을 입력해 주세요.");

        if (wordbookFolderRepository.existsByAccount_IdAndNormalizedFolderName(accountId, normalized))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 폴더명입니다.");

        int nextOrder = wordbookFolderRepository.findMaxSortOrderByAccountId(accountId) + 1;
        var entity = request.toWordbookFolder(nextOrder, normalized);

        try {
            var saved = wordbookFolderRepository.save(entity);
            return CreateWordbookFolderResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 폴더명입니다.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ListWordbookTermResponse list(ListWordbookTermRequest request) {
        Long accountId = request.getAccountId();
        Long folderId = request.getFolderId();

        boolean owns = wordbookFolderRepository.existsByIdAndAccount_Id(folderId, accountId);
        log.info("[list] owns? accountId={}, folderId={}, result={}", accountId, folderId, owns);
        if (!owns) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 폴더를 찾을 수 없습니다.");

        int pageIdx = Math.max(0, request.getPage());
        int size    = Math.min(Math.max(1, request.getPerPage()), 100);
        Sort sort   = parseSortOrDefault(request.getSort(), Sort.by(Sort.Order.desc("createdAt")));
        Pageable pageable = PageRequest.of(pageIdx, size, sort);

        log.info("[list] call repo: folderId={}, accountId={}, pageable={}", folderId, accountId, pageable);

        Page<WordbookTerm> paginatedList =
                wordbookTermRepository.findPageByFolderAndOwnerFetch(folderId, accountId, pageable);

        return ListWordbookTermResponse.from(paginatedList);
    }

    /** 폴더 전체 재정렬 */
    @Override
    @Transactional
    public void reorder(ReorderWordbookFoldersRequest request) {
        final Long accountId = request.getAccountId();
        final List<Long> orderedIds = Optional.ofNullable(request.getOrderedIds())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "ids가 필요합니다."));

        final Set<Long> dedup = new LinkedHashSet<>(orderedIds);
        if (dedup.size() != orderedIds.size())
            throw new ResponseStatusException(BAD_REQUEST, "ids에 중복이 포함되어 있습니다.");

        final List<WordbookFolder> all =
                wordbookFolderRepository.findAllByAccount_IdOrderBySortOrderAscIdAsc(accountId);
        if (all.isEmpty() && !orderedIds.isEmpty())
            throw new ResponseStatusException(BAD_REQUEST, "정렬할 폴더가 존재하지 않습니다.");

        final Set<Long> allIds = all.stream().map(WordbookFolder::getId).collect(Collectors.toSet());
        if (!allIds.equals(dedup))
            throw new ResponseStatusException(BAD_REQUEST, "ids가 계정의 폴더 전체 집합과 일치하지 않습니다.");

        int idx = 0;
        Map<Long, Integer> toOrder = new HashMap<>();
        for (Long id : orderedIds) toOrder.put(id, idx++);

        for (WordbookFolder folder : all) {
            Integer newOrder = toOrder.get(folder.getId());
            if (newOrder == null) throw new ResponseStatusException(NOT_FOUND, "요청하신 폴더를 찾을 수 없습니다.");
            if (!Objects.equals(folder.getSortOrder(), newOrder)) {
                folder.setSortOrder(newOrder);
            }
        }
        wordbookFolderRepository.saveAll(all);
    }

    @Override
    @Transactional
    public CreateWordbookTermResponse attachTerm(CreateWordbookTermRequest request) {
        Long accountId = request.getAccountId();
        Long folderId  = request.getFolderId();
        Long termId    = request.getTermId();

        if (accountId == null || folderId == null || termId == null || accountId <= 0 || folderId <= 0 || termId <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "잘못된 파라미터입니다.");
        }

        boolean owns = wordbookFolderRepository.existsByIdAndAccount_Id(folderId, accountId);
        if (!owns) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "폴더에 대한 권한이 없습니다.");

        Optional<WordbookTerm> existing = wordbookTermRepository
                .findByAccount_IdAndFolder_IdAndTerm_Id(accountId, folderId, termId);
        if (existing.isPresent()) {
            return CreateWordbookTermResponse.alreadyAttached(existing.get().getId(), folderId, termId);
        }

        if (!termRepository.existsById(termId)) {
            throw new ResponseStatusException(NOT_FOUND, "용어를 찾을 수 없습니다.");
        }

        WordbookFolder folderRef = wordbookFolderRepository.getReferenceById(folderId);
        Account accountRef           = accountRepository.getReferenceById(accountId);
        Term termRef                 = termRepository.getReferenceById(termId);

        WordbookTerm uwt = new WordbookTerm(accountRef, folderRef, termRef);
        WordbookTerm saved = wordbookTermRepository.save(uwt);
        return CreateWordbookTermResponse.created(saved.getId(), folderId, termId);
    }

    @Override
    @Transactional
    @Caching(evict = { @CacheEvict(value = "folderTerms", allEntries = true) })
    public MoveFolderTermsResponse moveTerms(
            Long accountId, Long sourceFolderId, Long targetFolderId, List<Long> termIds) {

        if (Objects.equals(sourceFolderId, targetFolderId)) {
            log.info("[moveTerms] SAME_FOLDER accountId={} folderId={} requestedIds={}",
                    accountId, sourceFolderId, termIds);
            final List<Long> ids = (termIds == null) ? Collections.emptyList() : termIds;
            return MoveFolderTermsResponse.sameFolder(sourceFolderId, ids);
        }
        if (termIds == null || termIds.isEmpty()) {
            return new MoveFolderTermsResponse(sourceFolderId, targetFolderId, 0, List.of(), List.of());
        }

        // 폴더 소유 검증
        var source = wordbookFolderRepository.findById(sourceFolderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "소스 폴더를 찾을 수 없습니다."));
        var target = wordbookFolderRepository.findById(targetFolderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "대상 폴더를 찾을 수 없습니다."));

        if (!Objects.equals(source.getAccount().getId(), accountId) ||
                !Objects.equals(target.getAccount().getId(), accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        // 입력 정규화
        List<Long> distinctTermIds = termIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 결과 집계 변수들 (★ 누락되기 쉬운 부분)
        int moved = 0;
        List<Long> movedTermIds = new ArrayList<>();
        List<MoveFolderTermsResponse.Skipped> skipped = new ArrayList<>();

        // 대상 폴더의 현재 최대 sortOrder (account 조건 제거 버전)
        int baseOrder = Optional.ofNullable(
                wordbookTermRepository.findMaxSortOrderByFolder(targetFolderId)
        ).orElse(0);
        int cursor = baseOrder;

        for (Long termId : distinctTermIds) {
            var srcOpt = wordbookTermRepository.findByFolder_IdAndTerm_Id(sourceFolderId, termId);
            if (srcOpt.isEmpty()) {
                skipped.add(new MoveFolderTermsResponse.Skipped(
                        termId, MoveFolderTermsResponse.Skipped.Reason.NOT_IN_SOURCE));
                continue;
            }

            boolean inTarget = wordbookTermRepository.existsByFolder_IdAndTerm_Id(targetFolderId, termId);
            if (inTarget) {
                skipped.add(new MoveFolderTermsResponse.Skipped(
                        termId, MoveFolderTermsResponse.Skipped.Reason.DUPLICATE_IN_TARGET));
                continue;
            }

            Term termRef = termRepository.getReferenceById(termId);
            var created = WordbookTerm.of(target, termRef, ++cursor); // account는 folder에서 자동 세팅
            wordbookTermRepository.save(created);

            wordbookTermRepository.deleteById(srcOpt.get().getId());
            moved++;
            movedTermIds.add(termId);
        }

        int dupe = (int) skipped.stream()
                .filter(s -> s.getReason() == MoveFolderTermsResponse.Skipped.Reason.DUPLICATE_IN_TARGET)
                .count();
        int notInSrc = (int) skipped.stream()
                .filter(s -> s.getReason() == MoveFolderTermsResponse.Skipped.Reason.NOT_IN_SOURCE)
                .count();
        int notFound = (int) skipped.stream()
                .filter(s -> s.getReason() == MoveFolderTermsResponse.Skipped.Reason.TERM_NOT_FOUND)
                .count();

        log.info("[moveTerms] accountId={} {} -> {} | requested={} moved={} skipped={} (dupe={}, notInSource={}, notFound={}) | movedTermIds={}",
                accountId, sourceFolderId, targetFolderId,
                distinctTermIds.size(), moved, skipped.size(), dupe, notInSrc, notFound, movedTermIds);

        return new MoveFolderTermsResponse(sourceFolderId, targetFolderId, moved, skipped, movedTermIds);
    }

    @Override
    @Transactional
    public RenameWordbookFolderResponse rename(RenameWordbookFolderRequest req) {
        final Long accountId = req.getAccountId();
        final Long folderId  = req.getFolderId();

        WordbookFolder folder = wordbookFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "폴더를 찾을 수 없습니다."));

        if (!folder.getAccount().getId().equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        String raw = req.getFolderName();
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "folderName은 공백일 수 없습니다.");
        }

        String normalized = normalizeLikeEntity(raw);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "folderName은 공백일 수 없습니다.");
        }
        if (normalized.length() > 50) {
            throw new ResponseStatusException(BAD_REQUEST, "folderName은 최대 50자입니다.");
        }

        // 변경 없음
        if (normalized.equals(folder.getNormalizedFolderName())) {
            return map(folder);
        }

        // 중복 체크 (자기 자신 제외)
        boolean dup = wordbookFolderRepository
                .existsByAccount_IdAndNormalizedFolderNameAndIdNot(accountId, normalized, folderId);
        if (dup) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "동일한 이름의 폴더가 이미 존재합니다.");
        }

        folder.setFolderName(raw);
        wordbookFolderRepository.save(folder); // @PreUpdate에서 updatedAt 갱신됨

        return map(folder);
    }

    @Override
    @Transactional
    public void deleteOne(Long accountId, DeleteMode mode, Long folderId, Long targetFolderId) {
        var folder = wordbookFolderRepository.findByIdAndAccount_Id(folderId, accountId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "폴더를 찾을 수 없습니다."));

        long count = wordbookTermRepository.countByFolderIdAndAccountId(folderId, accountId);

        switch (mode) {
            case FORBID -> {
                if (count > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "폴더에 항목이 있어 삭제할 수 없습니다.");
            }
            case DETACH -> {
                if (count > 0) wordbookTermRepository.deleteByFolderIdAndAccountId(folderId, accountId);
            }
            case MOVE -> {
                if (targetFolderId == null) throw new ResponseStatusException(BAD_REQUEST, "targetFolderId가 필요합니다.");
                if (Objects.equals(targetFolderId, folderId))
                    throw new ResponseStatusException(BAD_REQUEST, "targetFolderId가 삭제 대상과 같습니다.");
                wordbookFolderRepository.findByIdAndAccount_Id(targetFolderId, accountId)
                        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "대상 폴더를 찾을 수 없습니다."));
                if (count > 0) wordbookTermRepository.bulkUpdateMoveFolder(folderId, targetFolderId, accountId);
            }
            case PURGE -> {
                if (count > 0) wordbookTermRepository.deleteByFolderIdAndAccountId(folderId, accountId);
            }
        }

        wordbookFolderRepository.delete(folder);
        resequenceSortOrder(accountId);
    }

    @Override
    @Transactional
    public void deleteBulk(Long accountId, DeleteMode mode, List<Long> folderIds, Long targetFolderId) {
        if (folderIds == null || folderIds.isEmpty()) return;

        long owned = wordbookFolderRepository.countOwnedByIds(accountId, folderIds);
        if (owned != folderIds.size())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "소유하지 않은 폴더가 포함되어 있습니다.");

        if (mode == DeleteMode.MOVE) {
            if (targetFolderId == null)
                throw new ResponseStatusException(BAD_REQUEST, "targetFolderId가 필요합니다.");
            wordbookFolderRepository.findByIdAndAccount_Id(targetFolderId, accountId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "대상 폴더를 찾을 수 없습니다."));
            if (folderIds.contains(targetFolderId))
                throw new ResponseStatusException(BAD_REQUEST, "targetFolderId는 삭제 대상에 포함될 수 없습니다.");
        }

        if (mode == DeleteMode.FORBID) {
            for (Long fid : folderIds) {
                long c = wordbookTermRepository.countByFolderIdAndAccountId(fid, accountId);
                if (c > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "항목이 있는 폴더가 포함되어 있어 삭제할 수 없습니다.");
            }
        } else if (mode == DeleteMode.DETACH) {
            for (Long fid : folderIds) {
                wordbookTermRepository.deleteByAccountIdAndFolderIdIn(accountId, folderIds);
            }
        } else if (mode == DeleteMode.MOVE) {
            for (Long fid : folderIds) {
                wordbookTermRepository.bulkUpdateMoveFolder(fid, targetFolderId, accountId);
            }
        } else if (mode == DeleteMode.PURGE) {
            wordbookTermRepository.deleteByAccountIdAndFolderIdIn(accountId, folderIds);
        }

        wordbookFolderRepository.deleteByAccount_IdAndIdIn(accountId, folderIds);
        resequenceSortOrder(accountId);
    }

    @Override
    public TermIdsResult getAllTermIds(Long accountId, Long folderId) {
        // 소유권 검증
        boolean owns = wordbookFolderRepository.existsByIdAndAccount_Id(folderId, accountId);
        if (!owns) throw new ResponseStatusException(NOT_FOUND, "폴더를 찾을 수 없습니다.");

        // 단일 패스 조회(중복 제거 + 정렬)
        List<Long> ids = wordbookTermRepository
                .findDistinctTermIdsByFolderAndAccountOrderByTermIdAsc(folderId, accountId);

        int total = ids.size();
        boolean limitExceeded = total > maxTermIdsPerFolder;

        // 상한 초과 시 본문 termIds는 비워서 전송
        return new TermIdsResult(folderId, limitExceeded ? List.of() : ids, limitExceeded, maxTermIdsPerFolder, total);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportTermIdsResult collectExportTermIds(Long accountId, Long folderId, String memorization, List<String> includeTags, List<String> excludeTags, String sort, int hardLimit) {
        // 소유권 검증
        boolean owns = wordbookFolderRepository.existsByIdAndAccount_Id(folderId, accountId);
        if (!owns) throw new ResponseStatusException(NOT_FOUND, "폴더를 찾을 수 없습니다.");

        // 폴더 내 termId 전체 (중복 제거 + 정렬)
        List<Long> base = wordbookTermRepository
                .findDistinctTermIdsByFolderAndAccountOrderByTermIdAsc(folderId, accountId);

        final int totalBefore = base.size();
        if (totalBefore == 0) {
            return new ExportTermIdsResult(folderId, List.of(), 0, 0, false, hardLimit, 0);
        }

        // 필터 암기 상태
        List<Long> filtered = new ArrayList<>(base);
        if (memorization != null && !memorization.isBlank()) {
            var rows = learningProgressRepository.findByIdAccountIdAndIdTermIdIn(accountId, filtered);
            // 기본 LEARNING으로 보고 시작 -> MEMORIZED만 남기거나 반대로 필터
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
                folderId,
                finalIds,
                totalBefore,
                filteredOut,
                exceeded,
                limit,
                finalIds.size()
        );
    }

    @Override
    public AttachTermsBulkResponse attachTermsBulk(AttachTermsBulkRequest request) {
        final int MAX_BULK = 2000;  // 한 번에 처리 가능한 최대 개수
        final int BATCH_SIZE = 500; // saveAll 배치 크기

        Long accountId = request.accountId();
        Long folderId = request.folderId();
        List<Long> input = request.termIds() == null ? List.of() : request.termIds();

        // 기본 검증
        if (input.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "용어 ID 목록이 비어있습니다.");
        }
        if (input.size() > MAX_BULK) {
            throw new ResponseStatusException(BAD_REQUEST, "용어 ID 개수가 최대 허용 개수(" + MAX_BULK + ")를 초과했습니다.");
        }

        // 폴더 소유권 검증
        var folder = wordbookFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "폴더를 찾을 수 없습니다."));
        if (!Objects.equals(folder.getAccount().getId(), accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        // 입력 정규화(null 제거 + 중복 제거)
        List<Long> requestedDistinct = input.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 폴더 내 기존 termId 집합 -> 중복 판정 (스킵 정책)
        Set<Long> already = new HashSet<>(
                wordbookTermRepository.findDistinctTermIdsByFolderAndAccountOrderByTermIdAsc(folderId, accountId)
        );
        List<Long> duplicates = requestedDistinct.stream()
                .filter(already::contains)
                .toList();

        // 스킵 정책 고정(요청에 dedupeMode가 오더라도 중복은 항상 스킵)
        List<Long> candidates = requestedDistinct.stream()
                .filter(id -> !already.contains(id))
                .toList();

        // 용어 존재성 검증 -> invalidIds 분리
        var existingTerms = termRepository.findAllById(candidates);
        Set<Long>  existingIds = existingTerms.stream().map(Term::getId).collect(Collectors.toSet());

        List<Long> invalidIds = candidates.stream()
                .filter(id -> !existingIds.contains(id))
                .toList();

        // 실제 삽입 대상
        List<Long> toInsertIds = candidates.stream()
                .filter(existingIds::contains)
                .toList();

        // sortOrder 계산(기존 최대 뒤로 이어붙이기)
        Integer base = wordbookTermRepository.findMaxSortOrderByAccountAndFolder(accountId, folderId);
        int cursor = (base == null ? 0 : base);

        // 엔티티 생성 및 배치 저장
        int attached = 0;
        List<WordbookTerm> buffer = new ArrayList<>(Math.min(toInsertIds.size(), BATCH_SIZE));

        for (Long termId : toInsertIds) {
            Term termRef = termRepository.getReferenceById(termId);
            WordbookTerm uwt = WordbookTerm.of(folder, termRef, ++cursor); // account는 folder에서 세팅
            buffer.add(uwt);

            if (buffer.size() >= BATCH_SIZE) {
                wordbookTermRepository.saveAll(buffer);
                attached += buffer.size();
                buffer.clear();
            }
        }
        if (!buffer.isEmpty()) {
            var batch = List.copyOf(buffer);
            wordbookTermRepository.saveAll(batch);
            attached += batch.size();
            buffer.clear();
        }

        int requested = input.size();
        int skipped = duplicates.size();    // 이미 폴더에 있던 항목 수
        int failed = 0;                     // 개별 실패 처리 없음(예외 발생 시 전체 롤백)

        log.info("[attachTermsBulk] accountId={} folderId={} requested={} attached={} skipped={} invalid={}",
                accountId, folderId, requested, attached, skipped, invalidIds.size());

        return new AttachTermsBulkResponse(
                folderId,
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

    private RenameWordbookFolderResponse map(WordbookFolder f) {
        return RenameWordbookFolderResponse.builder()
                .id(f.getId())
                .folderName(f.getFolderName())
                .sortOrder(f.getSortOrder())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
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
        List<WordbookFolder> folders =
                wordbookFolderRepository.findAllByAccount_IdOrderBySortOrderAscIdAsc(accountId);

        // 0부터 증가하는 연속 정수로 sortOrder 재부여
        int i = 0;
        for (WordbookFolder f : folders) {
            if (f.getSortOrder() == null || !Objects.equals(f.getSortOrder(), i)) {
                f.setSortOrder(i);
            }
            i++;
        }

        // 변경사항 저장
        wordbookFolderRepository.saveAll(folders);
    }
}
