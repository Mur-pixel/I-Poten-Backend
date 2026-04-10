package com.cygnus.iptn.quiz_question.service;

import com.cygnus.iptn.quiz_question.entity.enums.QuestionType;
import com.cygnus.iptn.quiz_question.entity.QuizChoice;
import com.cygnus.iptn.quiz_question.entity.QuizQuestion;
import com.cygnus.iptn.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.iptn.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.iptn.quiz_question.service.request.CreateQuizChoiceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizChoiceServiceImpl implements QuizChoiceService {

    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizChoiceRepository quizChoiceRepository;

    @Override
    public List<QuizChoice> registerQuizChoices(Long quizQuestionId, List<CreateQuizChoiceRequest> requestList) {

        // 요청 리스트 검증(NPE 방지)
        if (requestList == null || requestList.isEmpty()) {
            throw new IllegalArgumentException("보기가 비어있습니다.");
        }

        // Quiz Question Id 검증
        Long targetQuestionId = requestList.get(0).getQuizQuestionId();
        QuizQuestion quizQuestion = quizQuestionRepository.findById(targetQuestionId)
                .orElseThrow(()-> new IllegalArgumentException("해당 퀴즈 문제가 없습니다."));

        // 정답 개수 유효성 검증
        long correctCount = requestList.stream().filter(CreateQuizChoiceRequest::isAnswer).count();

        if(quizQuestion.getQuestionType() == QuestionType.OX && correctCount != 1) {
            throw new IllegalArgumentException("OX 문제의 정답은 1개여야 합니다.");
        }

        if(quizQuestion.getQuestionType() == QuestionType.CHOICE && correctCount != 1) {
            throw new IllegalArgumentException("객관식 문제는 정답이 1개여야 합니다.");
        }

        List<QuizChoice> quizChoices = requestList.stream()
                .map(req -> QuizChoice.create(
                        quizQuestion,
                        req.getChoiceText(),
                        req.isAnswer()
                ))
                .toList();

        List<QuizChoice> savedQuizChoice = quizChoiceRepository.saveAll(quizChoices);
        return savedQuizChoice;
    }
}
