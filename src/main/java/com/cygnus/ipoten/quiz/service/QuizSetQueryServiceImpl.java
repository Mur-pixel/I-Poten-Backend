package com.cygnus.ipoten.quiz.service;

import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz.repository.QuizSetRepository;
import com.cygnus.ipoten.quiz_question.service.response.ChoiceQuestionRead;
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
                quizQuestionRepository.findByQuizSetIdAndQuestionTypeInOrderByIdAsc(
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
                "select qs.partType from QuizSet qs where qs.id = :id",
                QuizSetType.class
        ).setParameter("id", setId).getResultList();
        return r.stream().findFirst();
    }

    @Override
    public List<QuizQuestion> findInitialsQuestionsBySetId(Long setId) {
        return quizQuestionRepository.findByQuizSet_IdOrderByIdAsc(setId);
    }

}
