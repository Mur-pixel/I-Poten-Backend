package com.cygnus.ipoten.quiz_session.repository;

import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import com.cygnus.ipoten.quiz_set.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class QuizSessionTimelineRepositoryImpl implements QuizSessionTimelineRepositoryCustom {

    private final EntityManager em;

    @Override
    public Page<QuizSession> findTimelinePage(Long accountId, String q, QuizSetType part, Pageable pageable) {

        boolean hasQ = (q != null && !q.isBlank());

        String where =
                " WHERE s.account.id = :accountId " +
                        " AND s.sessionStatus = :submitted " +
                        (part != null ? " AND s.partType = :part " : "") +
                        (hasQ ?
                                " AND ( " +
                                        "   lower(coalesce(s.title,'')) like :q " +
                                        "   OR lower(coalesce(p.title,'')) like :q " +
                                        "   OR lower(coalesce(tc.name,'')) like :q " +
                                        " ) "
                                : "");

        String jpql =
                "SELECT s FROM QuizSession s " +
                        "LEFT JOIN FETCH s.parentSession p " +
                        "LEFT JOIN TermCategory tc " +
                        "  ON s.sourceType = com.cygnus.ipoten.quiz_session.entity.enums.SessionSourceType.TERM_CATEGORY " +
                        " AND s.sourceId = tc.id " +
                        where +
                        " ORDER BY COALESCE(s.submittedAt, s.startedAt) DESC";

        TypedQuery<QuizSession> dataQ = em.createQuery(jpql, QuizSession.class)
                .setParameter("accountId", accountId)
                .setParameter("submitted", SessionStatus.SUBMITTED);

        String countJpql =
                "SELECT COUNT(s) FROM QuizSession s " +
                        "LEFT JOIN s.parentSession p " +
                        "LEFT JOIN TermCategory tc " +
                        "  ON s.sourceType = com.cygnus.ipoten.quiz_session.entity.enums.SessionSourceType.TERM_CATEGORY " +
                        " AND s.sourceId = tc.id " +
                        where;
        TypedQuery<Long> cntQ = em.createQuery(countJpql, Long.class)
                .setParameter("accountId", accountId)
                .setParameter("submitted", SessionStatus.SUBMITTED);

        if (part != null) {
            dataQ.setParameter("part", part);
            cntQ.setParameter("part", part);
        }

        if (hasQ) {
            String like = "%" + q.toLowerCase() + "%";
            dataQ.setParameter("q", like);
            cntQ.setParameter("q", like);
        }

        dataQ.setFirstResult((int) pageable.getOffset());
        dataQ.setMaxResults(pageable.getPageSize());

        List<QuizSession> content = dataQ.getResultList();
        long total = cntQ.getSingleResult();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public long countSubmitted(Long accountId) {
        String jpql = "SELECT COUNT(s) FROM QuizSession s " +
                "WHERE s.account.id = :aid AND s.sessionStatus = :st";
        return em.createQuery(jpql, Long.class)
                .setParameter("aid", accountId)
                .setParameter("st", SessionStatus.SUBMITTED)
                .getSingleResult();
    }

    @Override
    public long countSubmittedRetry(Long accountId) {
        String jpql = "SELECT COUNT(s) FROM QuizSession s " +
                "WHERE s.account.id = :aid AND s.sessionStatus = :st " +
                "AND (s.parentSession IS NOT NULL " +
                "     OR s.sessionMode = com.cygnus.ipoten.quiz_session.entity.enums.SessionMode.WRONG_ONLY)";
        return em.createQuery(jpql, Long.class)
                .setParameter("aid", accountId)
                .setParameter("st", SessionStatus.SUBMITTED)
                .getSingleResult();
    }

    @Override
    public long sumTotalQuestionsOfSubmitted(Long accountId) {
        String jpql = "SELECT COALESCE(SUM(COALESCE(s.total, 0)), 0) " +
                "FROM QuizSession s " +
                "WHERE s.account.id = :aid AND s.sessionStatus = :st";
        return Optional.ofNullable(
                em.createQuery(jpql, Long.class)
                        .setParameter("aid", accountId)
                        .setParameter("st", SessionStatus.SUBMITTED)
                        .getSingleResult()
        ).orElse(0L);
    }

    @Override
    public long sumCorrectAnswersOfSubmitted(Long accountId) {
        String jpql = "SELECT COALESCE(SUM(CASE WHEN a.isCorrect = TRUE THEN 1 ELSE 0 END), 0) " +
                "FROM QuizSessionAnswer a " +
                "WHERE a.quizSession.account.id = :aid " +
                "AND a.quizSession.sessionStatus = :st";
        return Optional.ofNullable(
                em.createQuery(jpql, Long.class)
                        .setParameter("aid", accountId)
                        .setParameter("st", SessionStatus.SUBMITTED)
                        .getSingleResult()
        ).orElse(0L);
    }

    @Override
    public List<Object[]> countCorrectBySessionIds(Collection<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) return List.of();
        String jpql = "SELECT a.quizSession.id, " +
                "SUM(CASE WHEN a.isCorrect = TRUE THEN 1 ELSE 0 END) " +
                "FROM QuizSessionAnswer a " +
                "WHERE a.quizSession.id IN :ids " +
                "GROUP BY a.quizSession.id";
        return em.createQuery(jpql, Object[].class)
                .setParameter("ids", sessionIds)
                .getResultList();
    }

    @Override
    public List<Object[]> countAnswersBySessionIds(Collection<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) return List.of();
        String jpql = "SELECT a.quizSession.id, COUNT(a) " +
                "FROM QuizSessionAnswer a " +
                "WHERE a.quizSession.id IN :ids " +
                "GROUP BY a.quizSession.id";
        return em.createQuery(jpql, Object[].class)
                .setParameter("ids", sessionIds)
                .getResultList();
    }

    @Override
    public List<QuizSession> findRecentSessions(Long accountId, int limit) {
        String jpql =
                "SELECT s FROM QuizSession s " +
                        "WHERE s.account.id = :aid AND s.sessionStatus = :st " +
                        "ORDER BY COALESCE(s.submittedAt, s.startedAt) DESC";
        return em.createQuery(jpql, QuizSession.class)
                .setParameter("aid", accountId)
                .setParameter("st", SessionStatus.SUBMITTED)
                .setMaxResults(Math.max(1, limit))
                .getResultList();
    }
}
