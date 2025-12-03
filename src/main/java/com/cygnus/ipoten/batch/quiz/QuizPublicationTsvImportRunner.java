package com.cygnus.ipoten.batch.quiz;

import com.cygnus.ipoten.quiz.entity.QuizSet;
import com.cygnus.ipoten.quiz.entity.enums.JobRole;
import com.cygnus.ipoten.quiz.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz.repository.QuizSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizPublicationTsvImportRunner implements CommandLineRunner {

    private final QuizSetRepository quizSetRepository;

    private Map<String, Integer> buildHeaderIndex(String header) {
        if (header == null) throw new IllegalArgumentException("헤더가 비어 있음");
        String[] heads = header.split("\t");
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < heads.length; i++) {
            map.put(heads[i].trim().toLowerCase(Locale.ROOT), i);
        }
        require(map, "scheduled_date");
        require(map, "part_type");
        require(map, "set_title");
        // job_role_key, set_random, priority는 선택
        return map;
    }

    private void require(Map<String, Integer> map, String key) {
        if (!map.containsKey(key)) throw new IllegalArgumentException("헤더 누락: " + key);
    }

    private String get(String[] c, Map<String, Integer> col, String key) {
        Integer idx = col.get(key);
        if (idx == null || idx < 0 || idx >= c.length) return "";
        return c[idx] == null ? "" : c[idx].trim();
    }

    private boolean parseBool(String s) {
        if (s == null || s.isBlank()) return false;
        return "true".equalsIgnoreCase(s) || "1".equals(s) || "y".equalsIgnoreCase(s);
    }

    private QuizSet requireQuizSet(String title) {
        return quizSetRepository.findFirstByTitle(title)
                .orElseThrow(() -> new IllegalArgumentException("QuizSet not found for title: " + title));
    }

    @Override
    public void run(String... args) throws Exception {

    }
}
