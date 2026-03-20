package com.cygnus.ipoten.quiz_wrongnote.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizWrongNoteSchemaUpdater implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'quiz_wrong_note'
                  AND COLUMN_NAME = 'updated_at'
                """, Integer.class);

            if (count != null && count > 0) {
                return;
            }

            log.warn("[quiz_wrong_note] missing column `updated_at`; applying schema patch");

            jdbcTemplate.execute("""
                ALTER TABLE quiz_wrong_note
                ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                """);

            jdbcTemplate.update("""
                UPDATE quiz_wrong_note
                SET updated_at = COALESCE(submitted_at, CURRENT_TIMESTAMP(6))
                WHERE updated_at IS NULL
                """);

            log.info("[quiz_wrong_note] schema patch applied: added `updated_at`");
        } catch (Exception e) {
            log.error("[quiz_wrong_note] failed to ensure `updated_at` column", e);
            throw e;
        }
    }
}
