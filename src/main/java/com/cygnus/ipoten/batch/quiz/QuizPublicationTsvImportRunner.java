package com.cygnus.ipoten.batch.quiz;

import com.cygnus.ipoten.quiz.entity.QuizPublication;
import com.cygnus.ipoten.quiz.entity.QuizSet;
import com.cygnus.ipoten.quiz.entity.enums.JobRole;
import com.cygnus.ipoten.quiz.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz.repository.QuizPublicationRepository;
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
    private final QuizPublicationRepository quizPublicationRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        String pubPath = null;
        for (String arg : args) {
            if (arg.startsWith("--pub=")) pubPath = arg.substring("--pub=".length());
        }
        if (pubPath == null) {
            return;
        }

        File file = new File(pubPath);
        if (!file.exists()) {
            log.error("[PUB-IMPORT] file not found: {}", pubPath);
            return;
        }

        log.info("[PUB-IMPORT] importing publication map from {}", pubPath);

        int lineNo = 0, ok = 0, skip = 0, err = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("UTF-8")))) {
            String header = br.readLine(); lineNo++;
            Map<String, Integer> col = buildHeaderIndex(header);

            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) { skip++; continue; }
                String[] c = line.split("\t", -1);

                try {
                    LocalDate date = LocalDate.parse(get(c, col, "scheduled_date"));
                    QuizSetType part = QuizSetType.valueOf(get(c, col, "part_type").trim().toUpperCase());
                    String roleRaw = get(c, col, "job_role_key");
                    JobRole role = (roleRaw == null || roleRaw.isBlank()) ? JobRole.GENERAL : JobRole.from(roleRaw);

                    String setTitle = get(c, col, "set_title");
                    boolean setRandom = parseBool(get(c, col, "set_random"));

                    // 1) 세트 찾기(없으면 생성)
                    QuizSet set = findOrCreateSet(setTitle, setRandom);

                    // 2) 기존 발행 있으면 업데이트(세트 변경 허용), 없으면 생성
                    Optional<QuizPublication> old = quizPublicationRepository
                            .findFirstByScheduledDateAndPartTypeAndJobRoleAndActiveIsTrue(date, part, role);

                    if (old.isPresent()) {
                        QuizPublication pub = old.get();
                        // 동일 조합 유니크이므로 세트만 교체 가능
                        if (!pub.getQuizSet().getId().equals(set.getId())) {
                            quizPublicationRepository.delete(pub);
                            quizPublicationRepository.flush();
                            quizPublicationRepository.save(new QuizPublication(date, part, role, set));
                        }
                    } else {
                        quizPublicationRepository.save(new QuizPublication(date, part, role, set));
                    }

                    ok++;
                } catch (Exception ex) {
                    err++;
                    log.warn("[PUB-IMPORT] line {} error: {}", lineNo, ex.getMessage());
                }
            }
        }

        log.info("[PUB-IMPORT] done. ok={}, skip={}, err={}", ok, skip, err);
    }

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

    private QuizSet findOrCreateSet(String title, boolean random) {
        return quizSetRepository.findFirstByTitle(title)
                .orElseGet(() -> quizSetRepository.save(new QuizSet(title, random)));
    }
}
