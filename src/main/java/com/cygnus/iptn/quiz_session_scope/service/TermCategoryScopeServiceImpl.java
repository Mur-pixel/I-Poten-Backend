package com.cygnus.iptn.quiz_session_scope.service;

import com.cygnus.iptn.quiz_set.service.response.BuiltQuizSetResponse;
import com.cygnus.iptn.quiz_session_scope.value_objects.TermCategoryScope;
import com.cygnus.iptn.quiz_set.service.QuizSetService;
import com.cygnus.iptn.quiz_set.service.request.CreateQuizSetByCategoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TermCategoryScopeServiceImpl implements TermCategoryScopeService {

    private final QuizSetService quizSetService;

    @Override
    public BuiltQuizSetResponse buildQuizSet(TermCategoryScope termCategoryScope) {
        String title = "카테고리 퀴즈";
        CreateQuizSetByCategoryRequest request = termCategoryScope.toCreateQuizSetRequest(title);
        return quizSetService.registerQuizSetByCategoryReturningQuestions(request);
    }
}
