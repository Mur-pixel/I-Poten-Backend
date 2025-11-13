package com.cygnus.ipoten.quiz.repository;

import com.cygnus.ipoten.quiz.entity.UserQuizSession;
import com.cygnus.ipoten.quiz.entity.enums.QuizPartType;
import com.cygnus.ipoten.quiz.entity.enums.SessionStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
public class UserQuizSessionTimelineRepositoryImpl implements UserQuizSessionTimelineRepositoryCustom {

    private final EntityManager em;

    @Override
    public Page<UserQuizSession> findTimelinePage(Long accountId, String q, QuizPartType part, Pageable pageable) {
        String base =
                " FROM UserQuizSession s " +
                        " JOIN s.quizSet qs " +
                        " LEFT JOIN qs.category c " +
                        " WHERE s.account.id = :accountId " +
                        " AND s.sessionStatus = :submitted " +
                        (part != null ? " AND qs.partType = :part " : "") +
                        (StringUtils.hasText(q)
                                ? " AND (LOWER(qs.title) LIKE :kw OR LOWER(c.name) LIKE :kw) " : "");

        String jpql = "SELECT s " + base + " ORDER BY COALESCE(s.submittedAt, s.startedAt) DESC";
        TypedQuery<UserQuizSession> dataQ = em.createQuery(jpql, UserQuizSession.class)
                .setParameter("accountId", accountId)
                .setParameter("submitted", SessionStatus.SUBMITTED);

        String countJpql = "SELECT COUNT(s) " + base;
        TypedQuery<Long> cntQ = em.createQuery(countJpql, Long.class)
                .setParameter("accountId", accountId)
                .setParameter("submitted", SessionStatus.SUBMITTED);

        if (part != null) {
            dataQ.setParameter("part", part);
            cntQ.setParameter("part", part);
        }

        if (StringUtils.hasText(q)) {
            String kw = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            dataQ.setParameter("kw", kw);
            cntQ.setParameter("kw", kw);
        }

        dataQ.setFirstResult((int) pageable.getOffset());
        dataQ.setMaxResults(pageable.getPageSize());

        List<UserQuizSession> content = dataQ.getResultList();
        long total = cntQ.getSingleResult();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public long countSubmitted(Long accountId) {
        String jpql = "SELECT COUNT(s) FROM UserQuizSession s " +
                "WHERE s.account.id = :aid AND s.sessionStatus = :st";
        return em.createQuery(jpql, Long.class)
                .setParameter("aid", accountId)
                .setParameter("st", SessionStatus.SUBMITTED)
                .getSingleResult();
    }

    @Override
    public long countSubmittedRetry(Long accountId) {
        String jpql = "SELECT COUNT(s) FROM UserQuizSession s " +
                "WHERE s.account.id = :aid AND s.sessionStatus = :st " +
                "AND (s.parentSession IS NOT NULL " +
                "     OR s.sessionMode = com.cygnus.ipoten.quiz.entity.enums.SessionMode.WRONG_ONLY)";
        return em.createQuery(jpql, Long.class)
                .setParameter("aid", accountId)
                .setParameter("st", SessionStatus.SUBMITTED)
                .getSingleResult();
    }

    @Override
    public long sumTotalQuestionsOfSubmitted(Long accountId) {
        String jpql = "SELECT COALESCE(SUM(COALESCE(s.total, 0)), 0) " +
                "FROM UserQuizSession s " +
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
                "FROM SessionAnswer a " +
                "WHERE a.userQuizSession.account.id = :aid " +
                "AND a.userQuizSession.sessionStatus = :st";
        return Optional.ofNullable(
                em.createQuery(jpql, Long.class)
                        .setParameter("aid", accountId)
                        .setParameter("st", SessionStatus.SUBMITTED)
                        .getSingleResult()
        ).orElse(0L);
    }

    @Override
    public List<Object[]> findRecentRaw(Long accountId, int limit) {
        String jpql = "SELECT COALESCE(c.name, qs.title), COALESCE(s.submittedAt, s.startedAt) " +
                "FROM UserQuizSession s " +
                "JOIN s.quizSet qs " +
                "LEFT JOIN qs.category c " +
                "WHERE s.account.id = :aid AND s.sessionStatus = :st " +
                "ORDER BY COALESCE(s.submittedAt, s.startedAt) DESC";
        return em.createQuery(jpql, Object[].class)
                .setParameter("aid", accountId)
                .setParameter("st", SessionStatus.SUBMITTED)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public List<Object[]> countCorrectBySessionIds(Collection<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) return List.of();
        String jpql = "SELECT a.userQuizSession.id, " +
                "SUM(CASE WHEN a.isCorrect = TRUE THEN 1 ELSE 0 END) " +
                "FROM SessionAnswer a " +
                "WHERE a.userQuizSession.id IN :ids " +
                "GROUP BY a.userQuizSession.id";
        return em.createQuery(jpql, Object[].class)
                .setParameter("ids", sessionIds)
                .getResultList();
    }

    @Override
    public List<Object[]> countAnswersBySessionIds(Collection<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) return List.of();
        String jpql = "SELECT a.userQuizSession.id, COUNT(a) " +
                "FROM SessionAnswer a " +
                "WHERE a.userQuizSession.id IN :ids " +
                "GROUP BY a.userQuizSession.id";
        return em.createQuery(jpql, Object[].class)
                .setParameter("ids", sessionIds)
                .getResultList();
    }
}
