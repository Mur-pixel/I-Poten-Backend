package com.cygnus.ipoten.custom_term_recommendation.service;

import com.cygnus.ipoten.wordbook.service.WordbookFolderService;
import com.cygnus.ipoten.term.repository.JobRecommendedTermRepository;
import com.cygnus.ipoten.wordbook.repository.WordbookFolderRepository;
import com.cygnus.ipoten.wordbook.repository.WordbookTermRepository;
import com.cygnus.ipoten.wordbook.service.request.AttachTermsBulkRequest;
import com.cygnus.ipoten.wordbook.service.response.AttachTermsBulkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class JobGroupWordbookServiceImpl implements JobGroupWordbookService {

    private final WordbookFolderService wordbookFolderService;
    private final JobRecommendedTermRepository jobRecommendedTermRepository;
    private final WordbookFolderRepository wordbookFolderRepository;
    private final WordbookTermRepository wordbookTermRepository;

    @Override
    public AttachTermsBulkResponse attachJobGroupToFolder(Long accountId, Long folderId, String jobKey) {

        // 1) 폴더 소유자 검증
        var folder = wordbookFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Folder not found."));

        if (!folder.getAccount().getId().equals(accountId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission for this folder.");
        }

        // 2) 추천 term 목록 조회
        var recommendations =  jobRecommendedTermRepository.findByJobKeyOrderByRankNoAsc(jobKey);
        if (recommendations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recommended set not found for jobKey = " + jobKey);
        }

        var termIds = recommendations.stream()
                .map(r -> r.getTerm().getId())
                .toList();

        // 3) 이미 폴더에 있는 term 제거(중복 방지)
        var response = wordbookFolderService.attachTermsBulk(
                AttachTermsBulkRequest.of(accountId, folderId, termIds)
        );

        return response;
    }
}
