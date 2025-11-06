package com.cygnus.ipoten.quiz.service;

import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz.entity.QuizChoice;
import com.cygnus.ipoten.quiz.entity.QuizQuestion;
import com.cygnus.ipoten.quiz.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz.service.request.CreateQuizChoiceRequest;
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
        // Quiz Question Id 검증
        quizQuestionId = requestList.get(0).getQuizQuestionId();
        QuizQuestion quizQuestion = quizQuestionRepository.findById(quizQuestionId)
                .orElseThrow(()-> new IllegalArgumentException("해당 퀴즈 문제가 없습니다."));

        // choiceText 검증
        if(requestList == null || requestList.isEmpty()) {
            throw new IllegalArgumentException("보기가 비어있습니다.");
        }

        // 정답 개수 유효성 검증
        long correctCount = requestList.stream().filter(CreateQuizChoiceRequest::isAnswer).count();
        if(quizQuestion.getQuestionType() == QuestionType.OX && correctCount != 1) {
            throw new IllegalArgumentException("OX 문제의 정답은 1개여야 합니다.");
        }

        if(quizQuestion.getQuestionType() == QuestionType.CHOICE && correctCount != 1) {
            throw new IllegalArgumentException("객관식 문제는 정답이 1개여야 합니다.");
        }

        List<QuizChoice> quizChoices = requestList.stream()
                .map(req -> {
                    QuizChoice choice = new QuizChoice();
                    choice.setQuizQuestion(quizQuestion);
                    choice.setChoiceText(req.getChoiceText());
                    choice.setAnswer(req.isAnswer());
                    choice.setExplanation(req.getExplanation());
                    return choice;
                })
                .toList();

        List<QuizChoice> savedQuizChoice = quizChoiceRepository.saveAll(quizChoices);
        return savedQuizChoice;
    }
}
