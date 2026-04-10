package com.cygnus.iptn.quiz_question.service;

import com.cygnus.iptn.quiz_question.entity.enums.QuestionType;
import com.cygnus.iptn.quiz_question.entity.QuizQuestion;
import com.cygnus.iptn.quiz_question.entity.QuizTextAnswer;
import com.cygnus.iptn.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.iptn.quiz_question.repository.QuizTextAnswerRepository;
import com.cygnus.iptn.quiz_question.service.request.CreateQuizQuestionRequest;
import com.cygnus.iptn.quiz_question.service.response.CreateQuizQuestionResponse;
import com.cygnus.iptn.term_category.entity.TermCategory;
import com.cygnus.iptn.term.entity.Term;
import com.cygnus.iptn.term_category.repository.TermCategoryRepository;
import com.cygnus.iptn.term.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizQuestionServiceImpl implements QuizQuestionService {

    private final TermCategoryRepository termCategoryRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final TermRepository termRepository;
    private final QuizTextAnswerRepository quizTextAnswerRepository;

    @Override
    public CreateQuizQuestionResponse registerQuizQuestion(CreateQuizQuestionRequest request) {

        // Term 존재 확인
        Term term = termRepository.findById(request.getTermId())
                .orElseThrow(()-> new IllegalArgumentException("등록하고자 하는 용어가 없습니다."));

        // 카테고리 확인
        TermCategory termCategory = termCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));

        if (termCategory.getDepth() != 2) {
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
        } else if (request.getAnswerText() == null || request.getAnswerText().isBlank()) {
            throw new IllegalArgumentException("초성/텍스트형 문제는 텍스트를 반드시 입력해야 합니다.");
        }

        // QuizQuestion 생성 저장
        QuizQuestion quizQuestion = request.toQuizQuestion(term, termCategory);
        log.info("quiz question: {}", quizQuestion);
        QuizQuestion savedQuizQuestion = quizQuestionRepository.save(quizQuestion);
        
        // INITIALS/텍스트형인 경우 QuizTextAnswer 엔티티 생성
        if (request.getQuestionType() == QuestionType.INITIALS) {
            QuizTextAnswer quizTextAnswer = QuizTextAnswer.create(savedQuizQuestion, request.getQuestionText());
            quizTextAnswerRepository.save(quizTextAnswer);
        }

        // 응답 생성
        return CreateQuizQuestionResponse.from(savedQuizQuestion);
    }

}
