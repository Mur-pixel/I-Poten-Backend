package com.cygnus.ipoten.quiz_session_scope.service;

import com.cygnus.ipoten.quiz.service.response.BuiltQuizSetResponse;
import com.cygnus.ipoten.quiz_session_scope.value_objects.JobScope;
import com.cygnus.ipoten.quiz_set.service.QuizSetService;
import com.cygnus.ipoten.quiz_set.service.request.CreateQuizSetByJobRoleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobRoleScopeServiceImpl implements JobRoleScopeService {

    private final QuizSetService quizSetService;

    @Override
    @Transactional
    public BuiltQuizSetResponse buildQuizSet(JobScope jobScope) {
        CreateQuizSetByJobRoleRequest request = jobScope.toCreateQuizSetRequest();
        return quizSetService.registerQuizSetByJobRole(request);
    }
}
