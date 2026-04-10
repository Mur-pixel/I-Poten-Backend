package com.cygnus.iptn.batch.recommendation;

import com.cygnus.iptn.recommendation.entity.JobRecommendedTerm;
import com.cygnus.iptn.recommendation.repository.JobRecommendedTermRepository;
import com.cygnus.iptn.term.entity.Term;
import com.cygnus.iptn.term.repository.TermRepository;
import com.cygnus.iptn.term_category.entity.TermCategory;
import com.cygnus.iptn.term_category.repository.TermCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobRecommendedTermTsvImportRunner implements ApplicationRunner {

    private final TermRepository termRepository;
    private final JobRecommendedTermRepository jobRecommendedTermRepository;
    private final TermCategoryRepository termCategoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {

        // 배치 실행 여부 플래그
        // 기본값 false → 명시적으로 enabled=true를 줘야만 실행됨
        boolean enabled = Boolean.parseBoolean(
                getArg(args, "app.batch.job-recommendation.enabled", "false")
        );

        // 실수로 서버 기동 시 배치가 실행되는 것을 방지
        if (!enabled) {
            log.info("[JRT-IMPORT] disabled=true -> skip (use --app.batch.job-recommendation.enabled=true)");
            return;
        }

        // TSV 파일 경로
        String path = getArg(args, "app.batch.job-recommendation.path", null);
        if (path == null || path.isBlank()) {
            log.warn("[JRT-IMPORT] path가 비어있어서 스킵합니다. ex) --app.batch.job-recommendation.path=/.../jrt.tsv");
            return;
        }

        boolean failOnMissingTerm = Boolean.parseBoolean(getArg(
                args, "app.batch.job-recommendation.fail-on-missing-term", "false"
        ));

        Path file = Paths.get(path);
        if (!Files.exists(file)) {
            throw new IllegalStateException("[JRT-IMPORT] 파일이 존재하지 않습니다: " + file.toAbsolutePath());
        }

        List<JobRecommendedTermImportRow> rows = readTsv(file);
        log.info("[JRT-IMPORT] parsed rows={}", rows.size());

        // jobKey별 그룹핑
        Map<String, List<JobRecommendedTermImportRow>> grouped = rows.stream()
                .collect(Collectors.groupingBy(r -> normalize(r.getJobKey())));

        int totalInserted = 0;
        int totalMissingTerms = 0;

        for (var entry : grouped.entrySet()) {
            String jobKeyStr = entry.getKey();
            var groupRows = entry.getValue();

            var jobKey = com.cygnus.iptn.recommendation.entity.enums.JobKey.valueOf(jobKeyStr.toUpperCase(Locale.ROOT));

            // rank 오름차순
            groupRows.sort(Comparator.comparingInt(JobRecommendedTermImportRow::getRankNo));

            // 중복 rank 체크 (job 단위)
            Set<Integer> seenRank = new HashSet<>();
            for (var r : groupRows) {
                if (!seenRank.add(r.getRankNo())) {
                    log.warn("[JRT-IMPORT] duplicate rank in file. jobKey={}, rankNo={}, line={}",
                            jobKey, r.getRankNo(), r.getLineNo());
                }
            }

            // jobKey 단위로 기존 추천 데이터를 모두 삭제 후 재적재 (REPLACE 전략)
            int deleted = jobRecommendedTermRepository.deleteByJobKey(jobKey);
            log.info("[JRT-IMPORT] jobKey={} delete existing rows={}", jobKey, deleted);

            int insertedThisJob = 0;

            // (선택) 같은 termTitle+categoryId 중복 제거 (파일 품질 방어)
            Set<String> seenKey = new HashSet<>();

            for (var r : groupRows) {
                Long categoryId = r.getCategoryId();
                String title = r.getTermTitle();

                String dedupKey = jobKey + "|" + categoryId + "|" + normalize(title);
                if (!seenKey.add(dedupKey)) {
                    log.warn("[JRT-IMPORT] duplicate (job,category,title) -> skip. jobKey={}, categoryId={}, termTitle='{}', line={}",
                            jobKey, categoryId, title, r.getLineNo());
                    continue;
                }

                // term 조회: categoryId + title 정규화 매칭
                List<Term> candidates = termRepository.findAllByCategoryIdAndTitleNormalized(categoryId, title);

                // term이 존재하지 않는 경우
                // fail-on-missing-term=false 이면 해당 row만 스킵하고 계속 진행
                if (candidates.isEmpty()) {
                    totalMissingTerms++;
                    String msg = String.format(
                            "[JRT-IMPORT] Term not found -> jobKey=%s rankNo=%d categoryId=%d termTitle='%s' (line=%d)",
                            jobKey, r.getRankNo(), categoryId, title, r.getLineNo()
                    );
                    if (failOnMissingTerm) throw new IllegalStateException(msg);
                    log.warn(msg);
                    continue;
                }

                if (candidates.size() > 1) {
                    log.warn("[JRT-IMPORT] Term not unique -> skip. jobKey={}, categoryId={}, termTitle='{}', line={}, termIds={}",
                            jobKey, categoryId, title, r.getLineNo(),
                            candidates.stream().map(Term::getId).toList());
                    continue;
                }

                Term term = candidates.get(0);
                TermCategory categoryRef = termCategoryRepository.getReferenceById(categoryId);

                JobRecommendedTerm entity = JobRecommendedTerm.create(jobKey, term, categoryRef, r.getRankNo());
                jobRecommendedTermRepository.save(entity);

                insertedThisJob++;
            }

            totalInserted += insertedThisJob;
            log.info("[JRT-IMPORT] jobKey={} inserted={}", jobKey, insertedThisJob);
        }

        log.info("[JRT-IMPORT] DONE totalInserted={}, missingTerms={}, jobs={}",
                totalInserted, totalMissingTerms, grouped.size());
    }

    private List<JobRecommendedTermImportRow> readTsv(Path file) throws Exception {
        List<JobRecommendedTermImportRow> out = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            boolean headerSkipped = false;

            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;

                // 헤더 스킵: job_key\t rank_no\t category_id\t term_title\t memo
                if (!headerSkipped) {
                    headerSkipped = true;
                    String lower = line.toLowerCase(Locale.ROOT);
                    if (lower.contains("job_key") && lower.contains("rank_no") && lower.contains("category_id")) {
                        continue;
                    }
                }

                // 탭 기준 split (빈 칸 유지)
                String[] c = line.split("\t", -1);
                if (c.length < 4) {
                    throw new IllegalArgumentException("[JRT-IMPORT] invalid columns(<4). lineNo=" + lineNo + " raw=" + line);
                }

                String jobKey = c[0].trim();
                int rankNo = parseInt(c[1].trim(), "rank_no", lineNo);
                Long categoryId = parseLong(c[2].trim(), "category_id", lineNo);
                String termTitle = c[3].trim();
                String memo = (c.length >= 5) ? c[4].trim() : "";

                JobRecommendedTermImportRow row = new JobRecommendedTermImportRow(
                        jobKey, rankNo, categoryId, termTitle, memo, lineNo
                );
                row.validate();
                out.add(row);
            }
        }

        return out;
    }

    private int parseInt(String s, String col, int lineNo) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { throw new IllegalArgumentException("[JRT-IMPORT] invalid int. col=" + col + " lineNo=" + lineNo + " value=" + s); }
    }

    private Long parseLong(String s, String col, int lineNo) {
        try { return Long.parseLong(s); }
        catch (Exception e) { throw new IllegalArgumentException("[JRT-IMPORT] invalid long. col=" + col + " lineNo=" + lineNo + " value=" + s); }
    }

    private String normalize(String s) {
        return (s == null) ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private String getArg(ApplicationArguments args, String key, String defaultValue) {
        // 1) --key=value 형태
        if (args.containsOption(key)) {
            List<String> values = args.getOptionValues(key);
            if (values != null && !values.isEmpty()) return values.get(0);
            return "true";
        }
        // 2) 시스템 프로퍼티(-Dkey=) 혹은 env로도 대응
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) return sys;
        String env = System.getenv(key.replace('.', '_').toUpperCase(Locale.ROOT));
        if (env != null && !env.isBlank()) return env;

        return defaultValue;
    }
}
