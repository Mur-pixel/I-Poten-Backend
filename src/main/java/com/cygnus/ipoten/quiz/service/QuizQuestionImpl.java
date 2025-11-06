package com.cygnus.ipoten.quiz.service;

import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz.entity.QuizQuestion;
import com.cygnus.ipoten.quiz.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz.service.request.CreateQuizQuestionRequest;
import com.cygnus.ipoten.quiz.service.response.CreateQuizQuestionResponse;
import com.cygnus.ipoten.term.entity.Category;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term.repository.CategoryRepository;
import com.cygnus.ipoten.term.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizQuestionImpl implements QuizQuestionService {

    private final CategoryRepository categoryRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final TermRepository termRepository;

    @Override
    public CreateQuizQuestionResponse registerQuizQuestion(CreateQuizQuestionRequest request) {

        // 존재하지 않는 용어 ID 입력 시 에러 처리
        Term term = termRepository.findById(request.getTermId())
                .orElseThrow(()-> new IllegalArgumentException("등록하고자 하는 용어가 없습니다."));

        // 카테고리 조회
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        if (category.getDepth() != 2) {
            throw new IllegalArgumentException("카테고리 선택이 잘못 되었습니다.");
        }
        
        // questionText가 비어 있는 경우 예외 처리
        if(request.getQuestionText().isEmpty()) {
            throw new IllegalArgumentException("문제 입력란이 비어있습니다. 다시 등록하세요.");
        }

        // 문제의 답을 잘못 입력한 경우 예외 처리
        if(request.getQuestionType() == QuestionType.CHOICE) {
            if(request.getQuestionAnswer() <1 || request.getQuestionAnswer()>4 ) {
                throw new IllegalArgumentException("객관식 문제의 답은 1~4 사이의 숫자를 입력해야 합니다.");
            }
        } else if (request.getQuestionType() == QuestionType.OX) {
            if(request.getQuestionAnswer() != 1 && request.getQuestionAnswer() != 2) {
                throw new IllegalArgumentException("OX 문제의 정답은 1(참) 또는 2(거짓)이어야 합니다.");
            }
        }

        QuizQuestion quizQuestion = request.toQuizQuestion(term, category);
        log.info("quiz question: {}", quizQuestion);
        QuizQuestion savedQuizQuestion = quizQuestionRepository.save(quizQuestion);
        return CreateQuizQuestionResponse.from(savedQuizQuestion);
    }

}
