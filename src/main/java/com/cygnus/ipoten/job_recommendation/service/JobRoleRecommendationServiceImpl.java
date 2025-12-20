package com.cygnus.ipoten.job_recommendation.service;

import com.cygnus.ipoten.job.enums.JobRole;
import com.cygnus.ipoten.wordbook.service.WordbookService;
import com.cygnus.ipoten.job_recommendation.repository.JobRoleRecommendedTermRepository;
import com.cygnus.ipoten.wordbook.repository.WordbookRepository;
import com.cygnus.ipoten.wordbook.service.request.AttachTermsBulkRequest;
import com.cygnus.ipoten.wordbook.service.response.AttachTermsBulkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class JobRoleRecommendationServiceImpl implements JobRoleRecommendationService {

    private final WordbookService wordbookService;
    private final JobRoleRecommendedTermRepository jobRoleRecommendedTermRepository;
    private final WordbookRepository wordbookRepository;

    @Override
    public AttachTermsBulkResponse attachJobRoleRecommendationsToWordbook(Long accountId, Long wordbookId, JobRole jobRole) {

        // 1) 폴더 소유자 검증
        var wordbook = wordbookRepository.findById(wordbookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Wordbook not found."));

        if (!wordbook.getAccount().getId().equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission for this wordbook.");
        }

        // 2) 추천 term 목록 조회
        var recommendations =  jobRoleRecommendedTermRepository.findByJobRoleOrderByRankNoAsc(jobRole);
        if (recommendations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recommended set not found for jobKey = " + jobRole);
        }

        var termIds = recommendations.stream()
                .map(r -> r.getTerm().getId())
                .toList();

        // 3) 이미 폴더에 있는 term 제거(중복 방지)
        var response = wordbookService.attachTermsBulk(
                AttachTermsBulkRequest.of(accountId, wordbookId, termIds)
        );

        return response;
    }
}
