package com.cygnus.iptn.quiz_session_scope.value_objects;

import java.util.List;

public record WrongNoteScope (Long accountId, List<Long> questionIds) {}
