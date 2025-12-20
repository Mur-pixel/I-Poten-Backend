package com.cygnus.ipoten.quiz_session_scope.service;

import com.cygnus.ipoten.quiz.service.response.BuiltQuizSetResponse;
import com.cygnus.ipoten.quiz_session_scope.value_objects.WordbookScope;
import com.cygnus.ipoten.quiz_set.service.QuizSetService;
import com.cygnus.ipoten.quiz_set.service.request.CreateQuizSetByWordbookRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WordbookScopeServiceImpl implements WordbookScopeService {

    private final QuizSetService quizSetService;


    @Override
    public BuiltQuizSetResponse buildQuizSet(WordbookScope wordbookScope) {

        String title = "[Wordbook] #" + wordbookScope.getWordbookId();
        CreateQuizSetByWordbookRequest request = wordbookScope.toCreateQuizSetRequest(title);
        return quizSetService.registerQuizSetByWordbookReturningQuestions(request);
    }
}
