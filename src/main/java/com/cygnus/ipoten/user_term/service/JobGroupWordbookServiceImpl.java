package com.cygnus.ipoten.user_term.service;

import com.cygnus.ipoten.term.repository.JobRecommendedTermRepository;
import com.cygnus.ipoten.user_term.repository.UserWordbookFolderRepository;
import com.cygnus.ipoten.user_term.repository.UserWordbookTermRepository;
import com.cygnus.ipoten.user_term.service.request.AttachTermsBulkRequest;
import com.cygnus.ipoten.user_term.service.response.AttachTermsBulkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class JobGroupWordbookServiceImpl implements JobGroupWordbookService {

    private final UserWordbookFolderService userWordbookFolderService;
    private final JobRecommendedTermRepository jobRecommendedTermRepository;
    private final UserWordbookFolderRepository userWordbookFolderRepository;
    private final UserWordbookTermRepository userWordbookTermRepository;

    @Override
    public AttachTermsBulkResponse attachJobGroupToFolder(Long accountId, Long folderId, String jobKey) {

        // 1) 폴더 소유자 검증
        var folder = userWordbookFolderRepository.findById(folderId)
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
        var response = userWordbookFolderService.attachTermsBulk(
                AttachTermsBulkRequest.of(accountId, folderId, termIds)
        );

        return response;
    }
}
