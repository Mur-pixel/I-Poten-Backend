package com.cygnus.ipoten.survey.service;

import com.cygnus.ipoten.survey.entity.*;
import com.cygnus.ipoten.survey.repository.*;
import com.cygnus.ipoten.survey.service.request.SubmitSurveyAnswerRequest;
import com.cygnus.ipoten.survey.service.request.SubmitSurveyResponseRequest;
import com.cygnus.ipoten.survey.service.response.SubmitSurveyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 활성 설문 제출 처리 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SurveySubmitServiceImpl implements SurveySubmitService {

    private final SurveyFormRepository surveyFormRepository;
    private final SurveyQuestionRepository surveyQuestionRepository;
    private final SurveyQuestionOptionRepository surveyQuestionOptionRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final SurveyResponseAnswerRepository surveyResponseAnswerRepository;

    @Override
    public SubmitSurveyResponse submitActiveSurvey(Long accountId, SubmitSurveyResponseRequest request) {

        SurveyForm activeSurveyForm = surveyFormRepository.findFirstByActiveTrueOrderByVersionDesc()
                .orElseThrow(() -> new NoSuchElementException("현재 진행 중인 i-Poten 후기 설문이 없습니다."));

        List<SurveyQuestion> questions = surveyQuestionRepository
                .findBySurveyFormIdOrderByDisplayOrderAsc(activeSurveyForm.getId());

        if (questions.isEmpty()) {
            throw new IllegalArgumentException("제출 가능한 설문 문항이 없습니다.");
        }

        validateDuplicateQuestionCode(request.answers());

        Map<String, SurveyQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(
                        SurveyQuestion::getQuestionCode,
                        Function.identity()
                ));

        List<Long> questionIds = questions.stream()
                .map(SurveyQuestion::getId)
                .toList();

        Map<Long, Map<String, SurveyQuestionOption>> optionMapByQuestionId = questionIds.isEmpty()
                ? Collections.emptyMap()
                : surveyQuestionOptionRepository
                .findBySurveyQuestionIdInOrderBySurveyQuestionIdAscDisplayOrderAsc(questionIds)
                .stream()
                .collect(Collectors.groupingBy(
                        option -> option.getSurveyQuestion().getId(),
                        Collectors.toMap(
                                SurveyQuestionOption::getOptionCode,
                                Function.identity()
                        )
                ));

        validateRequiredQuestions(request.answers(), questions);
        validateAnswers(request.answers(), questionMap, optionMapByQuestionId);

        SurveyResponse surveyResponse = surveyResponseRepository.save(
                new SurveyResponse(activeSurveyForm, accountId, Instant.now())
        );

        List<SurveyResponseAnswer> answerEntities = request.answers().stream()
                .map(answer -> toAnswerEntity(
                        surveyResponse,
                        questionMap.get(answer.questionCode()),
                        answer
                ))
                .toList();

        surveyResponseAnswerRepository.saveAll(answerEntities);

        return new SubmitSurveyResponse(
                surveyResponse.getId(),
                activeSurveyForm.getCode(),
                surveyResponse.getSubmittedAt()
        );
    }

    private void validateDuplicateQuestionCode(List<SubmitSurveyAnswerRequest> answers) {
        Set<String> visited = new HashSet<>();

        for (SubmitSurveyAnswerRequest answer : answers) {
            if (!visited.add(answer.questionCode())) {
                throw new IllegalArgumentException("중복된 문항이 포함되어 있습니다: " + answer.questionCode());
            }
        }
    }

    private void validateRequiredQuestions(
            List<SubmitSurveyAnswerRequest> answers,
            List<SurveyQuestion> questions
    ) {
        Set<String> submitttedQuestionCodes = answers.stream()
                .map(SubmitSurveyAnswerRequest::questionCode)
                .collect(Collectors.toSet());

        List<String> missingRequiredQuestionCodes = questions.stream()
                .filter(SurveyQuestion::isRequired)
                .map(SurveyQuestion::getQuestionCode)
                .filter(questionCode -> !submitttedQuestionCodes.contains(questionCode))
                .toList();

        if (!missingRequiredQuestionCodes.isEmpty()) {
            throw new IllegalArgumentException("필수 문항이 누락되었습니다: " + String.join(", ", missingRequiredQuestionCodes));
        }
    }

    private void validateAnswers(
            List<SubmitSurveyAnswerRequest> answers,
            Map<String, SurveyQuestion> questionMap,
            Map<Long, Map<String, SurveyQuestionOption>> optionMapByQuestionId
    ) {
        for (SubmitSurveyAnswerRequest answer : answers) {
            SurveyQuestion question = questionMap.get(answer.questionCode());

            if (question == null) {
                throw new IllegalArgumentException("존재하지 않는 문항입니다: " + answer.questionCode());
            }

            switch (question.getQuestionType()) {
                case SINGLE_CHOICE -> validateSingleChoiceAnswer(answer, question, optionMapByQuestionId);
                case LINEAR_SCALE -> validateLinearScaleAnswer(answer, question);
                case LONG_TEXT -> validateLongTextAnswer(answer, question);
            }
        }
    }

    private void validateSingleChoiceAnswer(
            SubmitSurveyAnswerRequest answer,
            SurveyQuestion question,
            Map<Long, Map<String, SurveyQuestionOption>> optionMapByQuestionId
    ) {
        String selectedOptionCode = normalize(answer.selectedOptionCode());
        String textAnswer = normalize(answer.textAnswer());

        if (selectedOptionCode == null) {
            throw new IllegalArgumentException("객관식 문항은 선택지 코드가 필요합니다: " + question.getQuestionCode());
        }
        if (answer.scaleValue() != null || textAnswer != null) {
            throw new IllegalArgumentException("객관식 문항에는 선택지 코드만 제출할 수 있습니다: " + question.getQuestionCode());
        }

        Map<String, SurveyQuestionOption> optionMap = optionMapByQuestionId.getOrDefault(
                question.getId(),
                Collections.emptyMap()
        );

        if (!optionMap.containsKey(selectedOptionCode)) {
            throw new IllegalArgumentException("유효하지 않은 선택지입니다: " + selectedOptionCode);
        }
    }

    private void validateLinearScaleAnswer(
            SubmitSurveyAnswerRequest answer,
            SurveyQuestion question
    ) {
        String selectedOptionCode = normalize(answer.selectedOptionCode());
        String textAnswer = normalize(answer.textAnswer());

        if (answer.scaleValue() == null) {
            throw new IllegalArgumentException("척도형 문항은 점수 값이 필요합니다: " + question.getQuestionCode());
        }
        if (selectedOptionCode != null || textAnswer != null) {
            throw new IllegalArgumentException("척도형 문항에는 점수 값만 제출할 수 있습니다: " + question.getQuestionCode());
        }

        int scaleValue = answer.scaleValue();
        if (question.getScaleMin() == null || question.getScaleMax() == null) {
            throw new IllegalArgumentException("척도형 문항 설정이 올바르지 않습니다: " + question.getQuestionCode());
        }
        if (scaleValue < question.getScaleMin() || scaleValue > question.getScaleMax()) {
            throw new IllegalArgumentException(
                    "척도형 문항 값 범위를 벗어났습니다: " + question.getQuestionCode()
            );
        }
    }

    private void validateLongTextAnswer(
            SubmitSurveyAnswerRequest answer,
            SurveyQuestion question
    ) {
        String selectedOptionCode = normalize(answer.selectedOptionCode());
        String textAnswer = normalize(answer.textAnswer());

        if (textAnswer == null) {
            throw new IllegalArgumentException("주관식 문항은 텍스트 답변이 필요합니다: " + question.getQuestionCode());
        }
        if (selectedOptionCode != null || answer.scaleValue() != null) {
            throw new IllegalArgumentException("주관식 문항에는 텍스트만 제출할 수 있습니다: " + question.getQuestionCode());
        }
        if (textAnswer.length() > 2000) {
            throw new IllegalArgumentException("주관식 답변 길이 제한을 초과했습니다: " + question.getQuestionCode());
        }
    }

    private SurveyResponseAnswer toAnswerEntity(
            SurveyResponse surveyResponse,
            SurveyQuestion question,
            SubmitSurveyAnswerRequest answer
    ) {
        return new SurveyResponseAnswer(
                surveyResponse,
                question,
                normalize(answer.selectedOptionCode()),
                answer.scaleValue(),
                normalize(answer.textAnswer())
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
