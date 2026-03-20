package com.cygnus.ipoten.accountProfile.service;

import com.cygnus.ipoten.interview.repository.InterviewRepository;
import com.cygnus.ipoten.interview.entity.Interview;
import com.cygnus.ipoten.interviewee_profile.entity.IntervieweeProfile;
import com.cygnus.ipoten.quiz_session.repository.QuizSessionRepository;
import com.cygnus.ipoten.quiz_wrongnote.repository.QuizWrongNoteRepository;
import com.cygnus.ipoten.term_log.repository.TermSearchLogRepository;
import com.cygnus.ipoten.wordbook_learning.repository.LearningProgressRepository;
import com.cygnus.ipoten.wordbook_term.repository.WordbookTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MyPageSummaryService {

    private static final ZoneId ASIA_SEOUL = ZoneId.of("Asia/Seoul");

    private final QuizSessionRepository quizSessionRepository;
    private final InterviewRepository interviewRepository;
    private final QuizWrongNoteRepository quizWrongNoteRepository;
    private final WordbookTermRepository wordbookTermRepository;
    private final LearningProgressRepository learningProgressRepository;
    private final TermSearchLogRepository termSearchLogRepository;

    public Instant resolveLastActivityAt(Long accountId) {
        Instant quizLast = quizSessionRepository.findLatestActivityAt(accountId).orElse(null);
        Instant wrongNoteLast = quizWrongNoteRepository.findLatestUpdatedAtByAccountId(accountId).orElse(null);
        Instant wordbookTermLast = wordbookTermRepository.findLatestCreatedAtByAccountId(accountId).orElse(null);
        Instant learningLast = learningProgressRepository.findLatestActivityAtByAccountId(accountId).orElse(null);
        Instant searchLast = termSearchLogRepository.findLatestCreatedAtByActorKey(actorKeyForAccount(accountId)).orElse(null);

        LocalDateTime interviewLast = interviewRepository.findLatestCreatedAt(accountId).orElse(null);
        Instant interviewInstant = interviewLast == null
                ? null
                : interviewLast.atZone(ASIA_SEOUL).toInstant();

        return Stream.of(quizLast, interviewInstant, wrongNoteLast, wordbookTermLast, learningLast, searchLast)
                .filter(it -> it != null)
                .max(Instant::compareTo)
                .orElse(null);
    }

    public Optional<RepresentativeProfile> resolveRepresentativeProfile(Long accountId) {
        return interviewRepository.findRecentWithProfileByAccountId(accountId).stream()
                .map(Interview::getIntervieweeProfile)
                .filter(profile -> profile != null)
                .findFirst()
                .map(profile -> new RepresentativeProfile(
                        profile.getJob(),
                        profile.getCareer(),
                        buildRepresentativeLabel(profile)
                ));
    }

    private String buildRepresentativeLabel(IntervieweeProfile profile) {
        String job = normalizeJob(profile.getJob());
        String career = normalizeCareer(profile.getCareer());

        if (job == null && career == null) return null;
        if (job == null) return career;
        if (career == null) return job;
        return job + " " + career;
    }

    private String normalizeJob(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "BACKEND" -> "백엔드";
            case "FRONTEND" -> "프론트엔드";
            case "FULLSTACK" -> "풀스택";
            case "DEVOPS" -> "데브옵스";
            case "AI" -> "AI";
            case "WEBAPP" -> "웹앱";
            case "EMBEDDED" -> "임베디드";
            default -> raw.trim();
        };
    }

    private String normalizeCareer(String raw) {
        if (raw == null || raw.isBlank()) return null;
        if ("신입".equals(raw.trim())) return "주니어";
        return raw.trim();
    }

    private String actorKeyForAccount(Long accountId) {
        return "A_" + accountId;
    }

    public record RepresentativeProfile(
            String job,
            String career,
            String label
    ) {
    }
}
