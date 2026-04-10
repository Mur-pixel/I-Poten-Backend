package com.cygnus.iptn.survey.service;

import com.cygnus.iptn.survey.entity.SurveyForm;
import com.cygnus.iptn.survey.entity.SurveyQuestion;
import com.cygnus.iptn.survey.entity.SurveyQuestionOption;
import com.cygnus.iptn.survey.entity.SurveyQuestionType;
import com.cygnus.iptn.survey.repository.SurveyFormRepository;
import com.cygnus.iptn.survey.repository.SurveyQuestionOptionRepository;
import com.cygnus.iptn.survey.repository.SurveyQuestionRepository;
import com.cygnus.iptn.survey.service.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SurveyQueryServiceImpl implements SurveyQueryService {

    private final SurveyFormRepository surveyFormRepository;
    private final SurveyQuestionRepository surveyQuestionRepository;
    private final SurveyQuestionOptionRepository surveyQuestionOptionRepository;

    @Override
    public GetActiveSurveyResponse getActiveSurvey(Long accountId) {
        SurveyForm activeSurveyForm = surveyFormRepository.findFirstByActiveTrueOrderByVersionDesc()
                .orElseThrow(() -> new NoSuchElementException("현재 진행 중인 i-Ptn 후기 설문이 없습니다."));

        List<SurveyQuestion> questions = surveyQuestionRepository
                .findBySurveyFormIdOrderByDisplayOrderAsc(activeSurveyForm.getId());

        List<Long> questionIds = questions.stream()
                .map(SurveyQuestion::getId)
                .toList();

        Map<Long, List<SurveyQuestionOption>> optionMap = questionIds.isEmpty()
                ? Collections.emptyMap()
                : surveyQuestionOptionRepository
                .findBySurveyQuestionIdInOrderBySurveyQuestionIdAscDisplayOrderAsc(questionIds)
                .stream()
                .collect(Collectors.groupingBy(option -> option.getSurveyQuestion().getId()));

        List<SurveyQuestionResponse> questionResponses = questions.stream()
                .map(question -> toQuestionResponse(
                        question,
                        optionMap.getOrDefault(question.getId(), Collections.emptyList())
                ))
                .toList();

        return new GetActiveSurveyResponse(
                activeSurveyForm.getCode(),
                activeSurveyForm.getVersion(),
                activeSurveyForm.getTitle(),
                activeSurveyForm.getDescription(),
                questionResponses
        );
    }

    private SurveyQuestionResponse toQuestionResponse(
            SurveyQuestion question,
            List<SurveyQuestionOption> options
    ) {
        List<SurveyOptionResponse> optionResponses =
                question.getQuestionType() == SurveyQuestionType.SINGLE_CHOICE
                        ? options.stream()
                        .map(option -> new SurveyOptionResponse(
                                option.getOptionCode(),
                                option.getOptionLabel()
                        ))
                        .toList()
                        : null;

        SurveyScaleResponse scaleResponse =
                question.getQuestionType() == SurveyQuestionType.LINEAR_SCALE
                        ? new SurveyScaleResponse(
                        question.getScaleMin(),
                        question.getScaleMax(),
                        question.getScaleMinLabel(),
                        question.getScaleMaxLabel()
                )
                        : null;

        return new SurveyQuestionResponse(
                question.getQuestionCode(),
                question.getTitle(),
                question.getQuestionType(),
                question.isRequired(),
                optionResponses,
                scaleResponse
        );
    }
}