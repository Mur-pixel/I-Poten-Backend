package com.cygnus.ipoten.recommendation.service;

import com.cygnus.ipoten.recommendation.entity.JobRecommendedTerm;
import com.cygnus.ipoten.recommendation.entity.enums.JobKey;
import com.cygnus.ipoten.wordbook.service.WordbookService;
import com.cygnus.ipoten.recommendation.repository.JobRecommendedTermRepository;
import com.cygnus.ipoten.wordbook.repository.WordbookRepository;
import com.cygnus.ipoten.wordbook.service.request.AttachTermsBulkRequest;
import com.cygnus.ipoten.wordbook.service.response.AttachTermsBulkResponse;
import com.cygnus.ipoten.wordbook_term.repository.WordbookTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashSet;

@Service
@RequiredArgsConstructor
public class JobRecommendedTermServiceImpl implements JobRecommendedTermService {

    private final WordbookService wordbookService;
    private final JobRecommendedTermRepository jobRecommendedTermRepository;
    private final WordbookRepository wordbookRepository;
    private final WordbookTermRepository wordbookTermRepository;

    @Override
    @Transactional(readOnly = true)
    public List<JobRecommendedTerm> getJobRecommendations(JobKey jobKey) {
        if (jobKey == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "jobKey is required");
        }

        return jobRecommendedTermRepository.findAllWithTermByJobKeyOrderByRankNo(jobKey);
    }

    @Override
    @Transactional
    public AttachTermsBulkResponse attachJobRecommendationsToWordbook(Long accountId, Long wordbookId, JobKey jobKey) {

        var wordbook = wordbookRepository.findById(wordbookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wordbook not found."));

        if (!wordbook.getAccount().getId().equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission for this wordbook.");
        }

        if (jobKey == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "jobKey is required");
        }

        var recommendations = jobRecommendedTermRepository.findAllWithTermByJobKeyOrderByRankNo(jobKey);

        if (recommendations.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Recommended set not found for jobKey = " + jobKey
            );
        }

        var recommendedIds = recommendations.stream()
                .map(r -> r.getTerm() == null ? null : r.getTerm().getId())
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        int requested = recommendedIds.size();

        // account 기준이 아니라 폴더 기준으로 기존 term 조회
        var existingIds = new HashSet<>(
                wordbookTermRepository.findDistinctTermIdsByWordbookId(wordbookId)
        );

        var toAttach = recommendedIds.stream()
                .filter(id -> !existingIds.contains(id))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));

        int skippedByExisting = requested - toAttach.size();

        if (toAttach.isEmpty()) {
            return new AttachTermsBulkResponse(wordbookId, requested, 0, requested, 0, List.of());
        }

        var bulkResp = wordbookService.attachTermsBulk(
                AttachTermsBulkRequest.of(accountId, wordbookId, toAttach)
        );

        return new AttachTermsBulkResponse(
                wordbookId,
                requested,
                bulkResp.attached(),
                bulkResp.skipped() + skippedByExisting,
                bulkResp.failed(),
                bulkResp.invalidIds()
        );
    }
}
