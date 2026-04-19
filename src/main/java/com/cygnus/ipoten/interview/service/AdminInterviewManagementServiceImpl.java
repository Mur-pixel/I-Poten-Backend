package com.cygnus.ipoten.interview.service;

import com.cygnus.ipoten.account.entity.AccountRoleType;
import com.cygnus.ipoten.account.entity.RoleType;
import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.repository.AccountProfileRepository;
import com.cygnus.ipoten.interview.controller.request_form.AdminInterviewHistoryRequestForm;
import com.cygnus.ipoten.interview.controller.request_form.AdminInterviewUsersRequestForm;
import com.cygnus.ipoten.interview.controller.response_form.AdminInterviewDetailResponseForm;
import com.cygnus.ipoten.interview.controller.response_form.AdminInterviewHistoryItemResponseForm;
import com.cygnus.ipoten.interview.controller.response_form.AdminInterviewHistoryResponseForm;
import com.cygnus.ipoten.interview.controller.response_form.AdminInterviewOwnerResponseForm;
import com.cygnus.ipoten.interview.controller.response_form.AdminInterviewQuestionResponseForm;
import com.cygnus.ipoten.interview.controller.response_form.AdminInterviewSummaryResponseForm;
import com.cygnus.ipoten.interview.controller.response_form.AdminInterviewUserItemResponseForm;
import com.cygnus.ipoten.interview.controller.response_form.AdminInterviewUsersResponseForm;
import com.cygnus.ipoten.interview.entity.Interview;
import com.cygnus.ipoten.interview.entity.InterviewType;
import com.cygnus.ipoten.interview.repository.AdminInterviewQueryRepository;
import com.cygnus.ipoten.interview.repository.AdminInterviewQueryRepository.InterviewAggregateRow;
import com.cygnus.ipoten.interview.repository.AdminInterviewQueryRepository.InterviewSummaryRow;
import com.cygnus.ipoten.interview.repository.AdminInterviewQueryRepository.InterviewTypeRow;
import com.cygnus.ipoten.interview_result.entity.InterviewResult;
import com.cygnus.ipoten.interview_result.entity.InterviewResultDetail;
import com.cygnus.ipoten.interview_result.repository.InterviewResultDetailRepository;
import com.cygnus.ipoten.interview_result.repository.InterviewResultRepository;
import com.cygnus.ipoten.interview_score.entity.InterviewScore;
import com.cygnus.ipoten.interview_score.repository.InterviewScoreRepository;
import com.cygnus.ipoten.interviewQA.entity.InterviewQA;
import com.cygnus.ipoten.interviewQA.repository.InterviewQARepository;
import com.cygnus.ipoten.interviewee_profile.entity.IntervieweeProfile;
import com.cygnus.ipoten.interviewee_profile.entity.TechStack;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInterviewManagementServiceImpl implements AdminInterviewManagementService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime FAR_PAST = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime FAR_FUTURE = LocalDateTime.of(9999, 12, 31, 23, 59);

    private final AdminInterviewQueryRepository adminInterviewQueryRepository;
    private final AccountProfileRepository accountProfileRepository;
    private final InterviewScoreRepository interviewScoreRepository;
    private final InterviewResultRepository interviewResultRepository;
    private final InterviewResultDetailRepository interviewResultDetailRepository;
    private final InterviewQARepository interviewQARepository;

    @Override
    public AdminInterviewUsersResponseForm getInterviewUserList(AdminInterviewUsersRequestForm request) {
        int pageSize = request.resolvedPageSize();
        String qLike = buildLike(request.q());
        List<InterviewType> types = resolveTypes(request.types());
        LocalDateTime startDate = toStartOfDay(request.startDate());
        LocalDateTime endDate = toEndOfDay(request.endDate());

        List<Long> ids = adminInterviewQueryRepository.findCandidateAccountIds(
                request.lastAccountId(),
                qLike,
                types,
                startDate,
                endDate,
                PageRequest.of(0, pageSize + 1)
        );

        if (ids.isEmpty()) {
            return new AdminInterviewUsersResponseForm(List.of(), pageSize, false, null);
        }

        boolean hasNext = ids.size() > pageSize;
        List<Long> pageIds = hasNext ? ids.subList(0, pageSize) : ids;
        Long nextCursor = hasNext ? pageIds.get(pageIds.size() - 1) : null;

        Map<Long, AccountProfile> profileById = accountProfileRepository.findAllByAccountIdIn(pageIds).stream()
                .collect(Collectors.toMap(p -> p.getAccount().getId(), p -> p, (a, b) -> a));

        Map<Long, InterviewAggregateRow> aggById = adminInterviewQueryRepository
                .aggregateByAccountIds(pageIds, types, startDate, endDate).stream()
                .collect(Collectors.toMap(InterviewAggregateRow::getAccountId, r -> r, (a, b) -> a));

        Map<Long, List<InterviewType>> typesById = new HashMap<>();
        for (InterviewTypeRow row : adminInterviewQueryRepository.typesByAccountIds(pageIds, types, startDate, endDate)) {
            typesById.computeIfAbsent(row.getAccountId(), k -> new ArrayList<>()).add(row.getType());
        }

        List<AdminInterviewUserItemResponseForm> items = new ArrayList<>(pageIds.size());
        for (Long id : pageIds) {
            AccountProfile profile = profileById.get(id);
            if (profile == null) continue;

            InterviewAggregateRow agg = aggById.get(id);
            long count = agg == null ? 0L : Optional.ofNullable(agg.getCnt()).orElse(0L);
            LocalDateTime last = agg == null ? null : agg.getLast();

            items.add(new AdminInterviewUserItemResponseForm(
                    id,
                    profile.getEmail(),
                    profile.getNickname(),
                    formatInstant(profile.getAccount().getCreatedAt()),
                    resolveRoleName(profile),
                    count,
                    formatLocalDateTime(last),
                    typesById.getOrDefault(id, List.of())
            ));
        }

        return new AdminInterviewUsersResponseForm(items, pageSize, hasNext, nextCursor);
    }

    @Override
    public AdminInterviewHistoryResponseForm getInterviewHistory(Long userId, AdminInterviewHistoryRequestForm request) {
        AccountProfile profile = accountProfileRepository.findByAccountId(userId).orElse(null);
        if (profile == null) {
            return new AdminInterviewHistoryResponseForm(
                    null,
                    new AdminInterviewSummaryResponseForm(0L, null, 0),
                    List.of(),
                    request.resolvedPageSize(),
                    false,
                    null
            );
        }

        int pageSize = request.resolvedPageSize();
        List<InterviewType> types = resolveTypes(request.types());
        LocalDateTime startDate = toStartOfDay(request.startDate());
        LocalDateTime endDate = toEndOfDay(request.endDate());

        List<Interview> interviews = adminInterviewQueryRepository.findInterviewHistoryPage(
                userId,
                request.lastInterviewId(),
                types,
                startDate,
                endDate,
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = interviews.size() > pageSize;
        List<Interview> page = hasNext ? interviews.subList(0, pageSize) : interviews;
        Long nextCursor = hasNext && !page.isEmpty() ? page.get(page.size() - 1).getId() : null;

        List<AdminInterviewHistoryItemResponseForm> items = page.stream()
                .map(this::toHistoryItem)
                .toList();

        InterviewSummaryRow summaryRow = adminInterviewQueryRepository.summaryForAccount(userId, types, startDate, endDate);
        long totalCount = summaryRow == null || summaryRow.getCnt() == null ? 0L : summaryRow.getCnt();
        LocalDateTime lastAt = summaryRow == null ? null : summaryRow.getLast();
        int averageScore = averageScoreForAccount(userId, types, startDate, endDate);

        AdminInterviewOwnerResponseForm user = new AdminInterviewOwnerResponseForm(
                userId,
                profile.getEmail(),
                profile.getNickname(),
                resolveRoleName(profile),
                formatInstant(profile.getAccount().getCreatedAt())
        );
        AdminInterviewSummaryResponseForm summary = new AdminInterviewSummaryResponseForm(totalCount, formatLocalDateTime(lastAt), averageScore);

        return new AdminInterviewHistoryResponseForm(user, summary, items, pageSize, hasNext, nextCursor);
    }

    @Override
    public AdminInterviewDetailResponseForm getInterviewDetail(Long interviewId) {
        Interview interview = adminInterviewQueryRepository.findDetail(interviewId);
        if (interview == null) return null;

        Long accountId = interview.getAccount() != null ? interview.getAccount().getId() : null;
        AccountProfile profile = accountId == null ? null : accountProfileRepository.findByAccountId(accountId).orElse(null);

        AdminInterviewOwnerResponseForm owner = profile == null
                ? new AdminInterviewOwnerResponseForm(accountId, null, null, null, null)
                : new AdminInterviewOwnerResponseForm(
                        accountId,
                        profile.getEmail(),
                        profile.getNickname(),
                        resolveRoleName(profile),
                        formatInstant(profile.getAccount().getCreatedAt())
                );

        int totalScore = averageOfInterviewScore(interviewId);
        String summaryText = Optional.ofNullable(interviewResultRepository.findByInterviewId(interviewId))
                .map(InterviewResult::getOverallComment)
                .orElse("");

        List<AdminInterviewQuestionResponseForm> questions = buildQuestionList(interview);
        int questionCount = questions.size();
        int duration = computeDurationMinutes(interview, questionCount);
        List<String> techKeywords = interview.getIntervieweeProfile() == null ? List.of() : techStackLabels(interview.getIntervieweeProfile().getTechStack());

        return new AdminInterviewDetailResponseForm(
                interview.getId(),
                interview.getInterviewType(),
                deriveTitle(interview),
                deriveRoleLabel(interview),
                null, // status → 프론트가 finished 로 fallback
                interview.isFinished(),
                formatLocalDateTime(interview.getCreatedAt()),
                interview.isFinished() ? latestQaAt(interviewId) : null,
                duration,
                questionCount,
                totalScore,
                summaryText == null ? "" : summaryText,
                List.of(),
                List.of(),
                techKeywords,
                questions,
                null,
                owner
        );
    }

    /* ───────── helpers ───────── */

    private AdminInterviewHistoryItemResponseForm toHistoryItem(Interview i) {
        int score = averageOfInterviewScore(i.getId());
        int count = interviewQARepository.findByInterview_Id(i.getId()).size();
        int duration = computeDurationMinutes(i, count);

        String completedAt = i.isFinished() ? latestQaAt(i.getId()) : null;
        return new AdminInterviewHistoryItemResponseForm(
                i.getId(),
                i.getInterviewType(),
                deriveTitle(i),
                deriveRoleLabel(i),
                formatLocalDateTime(i.getCreatedAt()),
                completedAt,
                duration,
                count,
                score,
                null,
                i.isFinished()
        );
    }

    private List<AdminInterviewQuestionResponseForm> buildQuestionList(Interview interview) {
        List<InterviewQA> qaList = interviewQARepository.findByInterview_Id(interview.getId()).stream()
                .sorted(Comparator.comparing(qa -> Optional.ofNullable(qa.getCreatedAt()).orElse(LocalDateTime.MIN)))
                .toList();

        Map<String, InterviewResultDetail> detailByQuestion = Collections.emptyMap();
        InterviewResult result = interviewResultRepository.findByInterviewId(interview.getId());
        if (result != null) {
            detailByQuestion = interviewResultDetailRepository.findAllByInterviewResultId(result.getId()).stream()
                    .filter(d -> d.getQuestion() != null)
                    .collect(Collectors.toMap(
                            InterviewResultDetail::getQuestion,
                            d -> d,
                            (a, b) -> a
                    ));
        }

        List<AdminInterviewQuestionResponseForm> questions = new ArrayList<>(qaList.size());
        int order = 1;
        for (InterviewQA qa : qaList) {
            InterviewResultDetail detail = qa.getQuestion() == null ? null : detailByQuestion.get(qa.getQuestion());
            questions.add(new AdminInterviewQuestionResponseForm(
                    qa.getId(),
                    order++,
                    nullSafe(qa.getQuestion()),
                    nullSafe(qa.getAnswer()),
                    detail == null ? "" : nullSafe(detail.getFeedback()),
                    detail == null ? "" : nullSafe(detail.getCorrection()),
                    0,
                    List.of()
            ));
        }
        return questions;
    }

    private int averageScoreForAccount(Long accountId, List<InterviewType> types, LocalDateTime startDate, LocalDateTime endDate) {
        // 필터된 면접들의 평균 점수 (단순 평균)
        List<Interview> all = adminInterviewQueryRepository.findInterviewHistoryPage(
                accountId, null, types, startDate, endDate, PageRequest.of(0, 500)
        );
        if (all.isEmpty()) return 0;

        int sum = 0;
        int n = 0;
        for (Interview i : all) {
            if (!i.isFinished()) continue;
            int s = averageOfInterviewScore(i.getId());
            if (s > 0) {
                sum += s;
                n++;
            }
        }
        return n == 0 ? 0 : Math.round((float) sum / n);
    }

    private int averageOfInterviewScore(Long interviewId) {
        InterviewScore s = interviewScoreRepository.findByInterviewId(interviewId);
        if (s == null) return 0;
        int sum = s.getCommunication() + s.getProductivity() + s.getDocumentationSkills()
                + s.getFlexibility() + s.getProblemSolving() + s.getTechnicalSkills();
        if (sum <= 0) return 0;
        return Math.round(sum / 6f);
    }

    private int computeDurationMinutes(Interview interview, int fallbackQuestionCount) {
        List<InterviewQA> qas = interviewQARepository.findByInterview_Id(interview.getId());
        if (qas.isEmpty()) return 0;

        LocalDateTime min = interview.getCreatedAt();
        LocalDateTime max = null;
        for (InterviewQA qa : qas) {
            LocalDateTime created = qa.getCreatedAt();
            if (created == null) continue;
            if (max == null || created.isAfter(max)) max = created;
            if (min == null || created.isBefore(min)) min = created;
        }
        if (min == null || max == null) return 0;
        long minutes = java.time.Duration.between(min, max).toMinutes();
        if (minutes <= 0 && fallbackQuestionCount > 0) return Math.max(1, fallbackQuestionCount);
        return (int) Math.max(0, minutes);
    }

    private String latestQaAt(Long interviewId) {
        return interviewQARepository.findByInterview_Id(interviewId).stream()
                .map(InterviewQA::getCreatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(AdminInterviewManagementServiceImpl::formatLocalDateTime)
                .orElse(null);
    }

    private String deriveTitle(Interview interview) {
        String job = Optional.ofNullable(interview.getIntervieweeProfile())
                .map(IntervieweeProfile::getJob)
                .filter(s -> !s.isBlank())
                .orElse(null);
        InterviewType type = interview.getInterviewType();
        String typeLabel;
        if (type == null) {
            typeLabel = "면접";
        } else {
            typeLabel = switch (type) {
                case TECHNICAL -> "기술면접";
                case PERSONAL -> "인성면접";
                case COMPANY -> "기업면접";
            };
        }
        if (job == null) return typeLabel + " · " + interview.getInterviewSequence() + "회차";
        return job + " " + typeLabel;
    }

    private String deriveRoleLabel(Interview interview) {
        return Optional.ofNullable(interview.getIntervieweeProfile())
                .map(IntervieweeProfile::getJob)
                .orElse("");
    }

    private List<String> techStackLabels(List<TechStack> stacks) {
        if (stacks == null || stacks.isEmpty()) return List.of();
        return stacks.stream().map(Enum::name).toList();
    }

    private String resolveRoleName(AccountProfile profile) {
        AccountRoleType role = profile.getAccount().getAccountRoleType();
        RoleType type = role == null ? null : role.getRoleType();
        return type == null ? "USER" : type.name();
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static String formatLocalDateTime(LocalDateTime dt) {
        return dt == null ? null : dt.format(ISO);
    }

    private static String formatInstant(java.time.Instant instant) {
        return instant == null ? null : instant.atZone(KST).toLocalDateTime().format(ISO);
    }

    private static LocalDateTime toStartOfDay(LocalDate d) {
        return d == null ? FAR_PAST : d.atStartOfDay();
    }

    private static LocalDateTime toEndOfDay(LocalDate d) {
        return d == null ? FAR_FUTURE : d.atTime(23, 59, 59);
    }

    private static String buildLike(String q) {
        if (q == null || q.isBlank()) return "%";
        return "%" + q.toLowerCase() + "%";
    }

    private static List<InterviewType> resolveTypes(List<InterviewType> types) {
        if (types == null || types.isEmpty()) {
            return Arrays.asList(InterviewType.values());
        }
        EnumSet<InterviewType> set = EnumSet.noneOf(InterviewType.class);
        for (InterviewType t : types) if (t != null) set.add(t);
        return set.isEmpty() ? Arrays.asList(InterviewType.values()) : new ArrayList<>(set);
    }
}
