package com.cygnus.ipoten.quiz_set.service;

import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_set.entity.QuizSet;
import com.cygnus.ipoten.quiz_set.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz_set.repository.QuizSetRepository;
import com.cygnus.ipoten.quiz_question.service.response.ChoiceQuestionRead;
import com.cygnus.ipoten.quiz_set.service.response.ResolveQuizSetResult;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizSetQueryServiceImpl implements QuizSetQueryService {

    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final QuizSetRepository quizSetRepository;
    private final EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public List<ChoiceQuestionRead> findChoiceQuestionsBySetId(Long setId) {

        // 1) 세트의 CHOICE 문항(정렬 보장)
        List<QuizQuestion> questions =
                quizQuestionRepository.findByQuizSet_IdAndQuestionTypeInOrderByIdAsc(
                        setId, List.of(QuestionType.CHOICE, QuestionType.OX)
                );
        if (questions.isEmpty()) return List.of();

        // 2) 보기를 한 번에 로드(정렬: id ASC)
        List<Long> qIds = questions.stream().map(QuizQuestion::getId).toList();
        List<QuizChoice> choices = quizChoiceRepository.findByQuizQuestionIdInOrderByIdAsc(qIds);

        Map<Long, List<QuizChoice>> byQ = choices.stream()
                .collect(Collectors.groupingBy(c -> c.getQuizQuestion().getId(),
                        LinkedHashMap::new, Collectors.toList()));

        // 3) DTO 매핑 (answerIndex 1-based → 0-based 보정)
        List<ChoiceQuestionRead> out = new ArrayList<>(questions.size());
        for (QuizQuestion q : questions) {
            List<QuizChoice> cs = byQ.getOrDefault(q.getId(), List.of());

            List<String> choiceTexts = cs.stream()
                    .map(c -> Optional.ofNullable(c.getChoiceText()).orElse(""))
                    .toList();

            int correctIdx0 = 0;
            for (int i = 0; i < cs.size(); i++) {
                if (Boolean.TRUE.equals(cs.get(i).isAnswer())) {
                    correctIdx0 = i;
                    break;
                }
            }
            correctIdx0 = Math.min(Math.max(0, correctIdx0), Math.max(0, choiceTexts.size() - 1));

            String explanation = Optional.ofNullable(q.getExplanation()).orElse(null);

            out.add(new ChoiceQuestionRead(
                    q.getId(),
                    Optional.ofNullable(q.getQuestionText()).orElse(""),
                    choiceTexts,
                    correctIdx0,
                    explanation
            ));
        }
        return out;
    }

    @Override
    public List<Long> findQuestionIdsBySetId(Long setId) {
        return quizSetRepository.findQuestionIdsBySetId(setId);
    }

    @Override
    public Optional<QuizSetType> findPartTypeBySetId(Long setId) {
        List<QuizSetType> r = em.createQuery(
                "select qs.quizSetType from QuizSet qs where qs.id = :id",
                QuizSetType.class
        ).setParameter("id", setId).getResultList();
        return r.stream().findFirst();
    }

    @Override
    public List<QuizQuestion> findInitialsQuestionsBySetId(Long setId) {
        return quizQuestionRepository.findByQuizSet_IdAndQuestionTypeOrderByIdAsc(setId, QuestionType.INITIALS);
    }

    @Override
    @Transactional(readOnly = true)
    public ResolveQuizSetResult resolve(Long termCategoryId, QuizSetType typeFilter, DifficultyLevel levelFilter, int count) {
        if (termCategoryId == null) throw new IllegalArgumentException("termCategoryId is null");
        int c = Math.max(5, Math.min(20, count));

        // typeFilter(MIX=null) -> qt(null이면 필터 없음)
        QuestionType qt = null;
        if (typeFilter != null) {
            qt = switch (typeFilter) {
                case CHOICE -> QuestionType.CHOICE;
                case OX -> QuestionType.OX;
                case INITIALS -> QuestionType.INITIALS;
                case MIX -> null;
            };
        }

        // levelFilter(MIX=null)
        DifficultyLevel dl = (levelFilter == DifficultyLevel.MIX) ? null : levelFilter;

        Long setId = em.createQuery("""
        select q.quizSet.id
        from QuizQuestion q
        where q.termCategory.id = :cid
          and (:qt is null or q.questionType = :qt)
          and (:dl is null or q.difficulty = :dl)
        group by q.quizSet.id
        having count(q.id) >= :cnt
        order by q.quizSet.id desc
    """, Long.class)
                .setParameter("cid", termCategoryId)
                .setParameter("qt", qt)
                .setParameter("dl", dl)
                .setParameter("cnt", (long) c)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("resolve failed: no set matched"));

        QuizSet qs = quizSetRepository.findById(setId)
                .orElseThrow(() -> new NoSuchElementException("set not found: " + setId));

        Long total = em.createQuery("""
        select count(q.id)
        from QuizQuestion q
        where q.quizSet.id = :sid
          and (:qt is null or q.questionType = :qt)
          and (:dl is null or q.difficulty = :dl)
    """, Long.class)
                .setParameter("sid", setId)
                .setParameter("qt", qt)
                .setParameter("dl", dl)
                .getSingleResult();

        return new ResolveQuizSetResult(qs.getId(), qs.getTitle(), total.intValue());
    }
}
