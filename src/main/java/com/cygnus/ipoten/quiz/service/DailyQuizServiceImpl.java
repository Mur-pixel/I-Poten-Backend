package com.cygnus.ipoten.quiz.service;

import com.cygnus.ipoten.quiz.entity.enums.JobRole;
import com.cygnus.ipoten.quiz.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz.repository.QuizPublicationRepository;
import com.cygnus.ipoten.quiz.service.response.BuiltQuizSetResponse;
import com.cygnus.ipoten.quiz.service.response.InitialsQuestionRead;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyQuizServiceImpl implements DailyQuizService {
    private final QuizPublicationRepository publicationRepository;
    private final EntityManager em;

    @Transactional(readOnly = true)
    public BuiltQuizSetResponse resolve(LocalDate date, QuizSetType part, JobRole role) {

        List<JobRole> roles = (role == JobRole.GENERAL)
                ? List.of(JobRole.GENERAL)
                : List.of(role, JobRole.GENERAL);

        var pub = publicationRepository
                .findExact(date, part, roles)
                .or(() -> publicationRepository.findLatestOnOrBefore(date, part, roles))
                .orElseThrow(() -> new IllegalArgumentException("해당 날짜/파트 공개 세트가 없습니다."));

        Long setId = pub.getQuizSet().getId();

        List<Long> qids = em.createQuery(
                "select q.id from QuizQuestion q " +
                   "where q.quizSet.id = :sid " +
                   "order by coalesce(q.orderIndex, q.id)",
                Long.class
        ).setParameter("sid", setId).getResultList();

        return BuiltQuizSetResponse.builder()
                .quizSetId(setId)
                .questionIds(qids)
                .title(pub.getQuizSet().getTitle())
                .isRandom(pub.getQuizSet().isRandom())
                .totalQuestions(qids.size())
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public List<InitialsQuestionRead> loadInitialsQuestions(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }

        // 1) 한 번에 로드
        var rows = em.createQuery(
                "select new com.cygnus.ipoten.quiz.service.response.InitialsQuestionRead(" +
                        "q.id, q.questionText, q.answerText) " +
                        "from QuizQuestion q " +
                        "where q.id in :ids",
                InitialsQuestionRead.class
        ).setParameter("ids", questionIds).getResultList();

        // 2) 원래 ID 순서 유지해서 정렬
        Map<Long, InitialsQuestionRead> byId = rows.stream().collect(Collectors.toMap(InitialsQuestionRead::id, Function.identity()));

        List<InitialsQuestionRead> ordered = new ArrayList<>(questionIds.size());
        for (Long id : questionIds) {
            var r = byId.get(id);
            if (r != null) {
                ordered.add(r);
            }
            return ordered;
        }
        return List.of();
    }
}