package com.cygnus.ipoten.user_term.service;

import com.cygnus.ipoten.redis_cache.RedisCacheService;
import com.cygnus.ipoten.user_term.controller.response_form.FolderSummaryResponseForm;
import com.cygnus.ipoten.user_term.controller.response_form.MyFolderListResponseForm;
import com.cygnus.ipoten.user_term.entity.enums.FolderTermSort;
import com.cygnus.ipoten.user_term.repository.UserWordbookFolderRepository;
import com.cygnus.ipoten.user_term.repository.UserWordbookTermRepository;
import com.cygnus.ipoten.user_term.repository.projection.FolderCountRow;
import com.cygnus.ipoten.user_term.repository.UserTermProgressRepository;
import com.cygnus.ipoten.term.repository.TermTagRepository;
import com.cygnus.ipoten.user_term.repository.projection.FolderStatsRow;
import com.cygnus.ipoten.user_term.repository.query.UserWordbookTermQueryRepository;
import com.cygnus.ipoten.user_term.service.response.Paged;
import com.cygnus.ipoten.user_term.service.view.FolderTermRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserWordbookFolderQueryServiceImpl implements UserWordbookFolderQueryService {

    private final UserWordbookFolderRepository userWordbookFolderRepository;
    private final UserWordbookTermRepository userWordbookTermRepository;
    private final UserTermProgressRepository userTermProgressRepository;
    private final TermTagRepository termTagRepository;
    private final RedisCacheService redisCacheService;
    private final UserWordbookTermQueryRepository userWordbookTermQueryRepository;

    private final EntityManager em;

    @Value("${ebook.max.termids.per.folder:5000}")
    private int maxTermIdsPerFolder;

    private String key(Long accountId) {
        return "folders:" + accountId;
    }

    private static final Duration TTL = Duration.ofMinutes(10);

    private String keyV1(Long accountId) { return "folders:" + accountId; }
    private String keyVStats(Long accountId) { return "folders:stats:" + accountId; }

    @Override
    public MyFolderListResponseForm getMyFolders(Long accountId) {
        // 1) 캐시 히트 시 반환
        var cached = redisCacheService.getValueByKey(key(accountId), MyFolderListResponseForm.class);
        if (cached != null) {
            return cached;
        }

        // 2) DB 조회
        List<FolderCountRow> rows = userWordbookFolderRepository.findMyFoldersWithFavoriteCount(accountId);
        var response = MyFolderListResponseForm.builder()
                .folders(rows.stream()
                        .map(r -> FolderSummaryResponseForm.builder()
                                .id(r.id()).name(r.name()).termCount(r.termCount())
                                .build())
                        .toList())
                .build();

        // 3) 캐싱
        redisCacheService.setKeyAndValue(key(accountId), response, TTL);
        return response;
    }

    @Override
    public MyFolderListResponseForm getMyFoldersWithStats(Long accountId) {

        var cacheKey = keyVStats(accountId);
        var cached = redisCacheService.getValueByKey(cacheKey, MyFolderListResponseForm.class);
        if (cached != null) {
            return cached;
        }

        List<FolderStatsRow> rows = userWordbookFolderRepository.findMyFoldersWithStats(accountId);
        var response = MyFolderListResponseForm.builder()
                .folders(rows.stream().map(r ->
                        FolderSummaryResponseForm.builder()
                                .id(r.getId())
                                .name(r.getFolderName())
                                .termCount(Optional.ofNullable(r.getTermCount()).orElse(0L))
                                .learnedCount(Optional.ofNullable(r.getLearnedCount()).orElse(0L))
                                .updatedAt(r.getUpdatedAt())
                                .lastStudiedAt(r.getLastStudiedAt())
                                .build()
                ).toList())
                .build();
        redisCacheService.setKeyAndValue(cacheKey, response, TTL);
        return response;
    }

    @Override
    public Paged<FolderSummaryResponseForm> getMyFoldersWithStatsPaged(
            Long accountId, int page, int perPage, String sort, String q) {

        // 1) sort 화이트리스트 매핑 (SQL 인젝션 방지)
        String[] sp = (sort == null ? "sortOrder,asc" : sort).split(",");
        String key = sp[0].trim();
        String dir = (sp.length > 1 ? sp[1].trim().toUpperCase() : "ASC");
        if (!dir.equals("ASC") && !dir.equals("DESC")) dir = "ASC";

        Map<String, String> cols = Map.of(
                "sortOrder",   "f.sort_order",
                "id",          "f.id",
                "name",        "f.folder_name",
                "folderName",  "f.folder_name",
                "termCount",   "termCount",
                "learnedCount","learnedCount",
                "updatedAt",   "updatedAt",
                "lastStudiedAt","lastStudiedAt"
        );
        String orderBy = cols.getOrDefault(key, "f.sort_order") + " " + dir + ", f.id ASC";

        // 2) 공통 FROM/GROUP BY
        String where = " WHERE f.account_id = :acc " + (q != null && !q.isBlank() ? " AND f.folder_name LIKE :q " : "");
        String group = " GROUP BY f.id, f.folder_name, f.sort_order, f.updated_at ";

        String select =
                "SELECT " +
                        "  f.id AS id, " +
                        "  f.folder_name AS name, " +
                        "  COUNT(uwt.term_id) AS termCount, " +
                        "  COALESCE(SUM(CASE WHEN utp.status = 'MEMORIZED' THEN 1 ELSE 0 END), 0) AS learnedCount, " +
                        "  COALESCE(GREATEST(COALESCE(MAX(uwt.updated_at), f.updated_at), f.updated_at), f.updated_at) AS updatedAt " +
                        ", MAX(utp.last_studied_at) AS lastStudiedAt " +
                        "FROM user_wordbook_folder f " +
                        "LEFT JOIN user_wordbook_term uwt ON uwt.folder_id = f.id " +
                        "LEFT JOIN user_term_progress utp ON utp.account_id = f.account_id AND utp.term_id = uwt.term_id " +
                        where +
                        group;

        // 3) total (그룹 행 수) — 서브쿼리 카운트
        String countSql = "SELECT COUNT(*) FROM (" + select + ") t";
        Query countQ = em.createNativeQuery(countSql)
                .setParameter("acc", accountId);
        if (q != null && !q.isBlank()) countQ.setParameter("q", "%" + q.trim() + "%");
        long total = ((Number) countQ.getSingleResult()).longValue();

        // 4) page query + 정렬 + LIMIT/OFFSET
        String pageSql = select + " ORDER BY " + orderBy + " LIMIT :limit OFFSET :offset";
        Query dataQ = em.createNativeQuery(pageSql)
                .setParameter("acc", accountId)
                .setParameter("limit", perPage)
                .setParameter("offset", page * perPage);

        if (q != null && !q.isBlank()) dataQ.setParameter("q", "%" + q.trim() + "%");

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQ.getResultList();

        List<FolderSummaryResponseForm> items = rows.stream()
                .map(r -> FolderSummaryResponseForm.builder()
                        .id(((Number) r[0]).longValue())
                        .name((String) r[1])
                        .termCount(((Number) r[2]).longValue())
                        .learnedCount(((Number) r[3]).longValue())
                        .updatedAt((r[4] instanceof java.sql.Timestamp ts) ? ts.toLocalDateTime() : (LocalDateTime) r[4])
                        .lastStudiedAt((r[5] instanceof java.sql.Timestamp ts2) ? ts2.toLocalDateTime() : (LocalDateTime) r[5])
                        .build())
                .toList();

        return new Paged<>(items, total);
    }

    @Override
    public void evictMyFoldersCache(Long accountId) {
        redisCacheService.deleteByKey(key(accountId));
    }

    @Override
    public void evictMyFoldersStatsCache(Long accountId) {
        redisCacheService.deleteByKey("folders:stats:" + accountId);
    }

    @Override
    public boolean existsByIdAndAccountId(Long folderId, Long accountId) {
        return userWordbookFolderRepository.existsByIdAndAccount_Id(folderId, accountId);
    }

    /**
     * eBook 내보내기용 termId 수집 (읽기 전용)
     * - 암기 상태/태그 포함/제외 필터 및 정렬, 상한 처리 포함
     */
    @Override
    public UserWordbookFolderService.ExportTermIdsResult collectExportTermIds(Long accountId, Long folderId,
                                                                              String memorization,
                                                                              List<String> includeTags,
                                                                              List<String> excludeTags,
                                                                              String sort,
                                                                              int hardLimit) {
        // 소유권 검증
        boolean owns = userWordbookFolderRepository.existsByIdAndAccount_Id(folderId, accountId);
        if (!owns) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "폴더를 찾을 수 없습니다.");

        // 폴더 내 termId 전체 (중복 제거 + 정렬)
        List<Long> base = userWordbookTermRepository
                .findDistinctTermIdsByFolderAndAccountOrderByTermIdAsc(folderId, accountId);

        final int totalBefore = base.size();
        if (totalBefore == 0) {
            return new UserWordbookFolderService.ExportTermIdsResult(folderId, List.of(), 0, 0, false, hardLimit, 0);
        }

        // 필터 암기 상태
        List<Long> filtered = new ArrayList<>(base);
        if (memorization != null && !memorization.isBlank()) {
            var rows = userTermProgressRepository.findByIdAccountIdAndIdTermIdIn(accountId, filtered);
            Map<Long, String> statusMap = new HashMap<>();
            for (Long id : filtered) statusMap.put(id, "LEARNING"); // 기본값
            rows.forEach(p -> statusMap.put(p.getId().getTermId(), p.getStatus().name()));

            final String want = memorization.toUpperCase(Locale.ROOT);
            filtered = filtered.stream()
                    .filter(id -> Objects.equals(statusMap.get(id), want))
                    .collect(Collectors.toList());
        }

        // 태그 포함/제외 (include 먼저, exclude 다음)
        if (includeTags != null && !includeTags.isEmpty()) {
            var rows = termTagRepository.findTermIdAndTagNameByTermIdIn(filtered);
            Map<Long, Set<String>> byTerm = new HashMap<>();
            rows.forEach(r -> byTerm.computeIfAbsent(r.getTermId(), k -> new HashSet<>()).add(r.getTagName()));
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
            rows.forEach(r -> byTerm.computeIfAbsent(r.getTermId(), k -> new HashSet<>()).add(r.getTagName()));
            filtered = filtered.stream()
                    .filter(id -> {
                        Set<String> have = byTerm.getOrDefault(id, Set.of());
                        for (String tag : excludeTags) {
                            if (have.contains(tag)) return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        // 정렬 : termId ASC/DESC
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

        return new UserWordbookFolderService.ExportTermIdsResult(
                folderId,
                finalIds,
                totalBefore,
                filteredOut,
                exceeded,
                limit,
                finalIds.size()
        );
    }

    // 단어장 폴더에 있는 총 단어 개수를 즉시 확인하기
    @Override
    @Transactional(readOnly = true)
    public long countTermsInFolderOrThrow(Long accountId, Long folderId) {
        userWordbookFolderRepository.findByIdAndAccount_Id(folderId, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "폴더를 찾을 수 없습니다."));

        long count = userWordbookTermRepository.countByFolderIdAndAccountId(folderId, accountId);
        log.debug("[folder:count] accountId={}, folderId={}, count={}", accountId, folderId, count);
        return count;
    }

    @Override
    public UserWordbookTermQueryRepository.PageResult<FolderTermRow> listFolderTerms(
            Long accountId, Long folderId, int page, int perPage, String sortParam
    ) {
        FolderTermSort sort = FolderTermSort.fromParam(sortParam);
        return userWordbookTermQueryRepository.findFolderTerms(accountId, folderId, page, perPage, sort);
    }
}