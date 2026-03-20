package com.cygnus.ipoten.inquiry.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InquirySchemaUpdater implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureTable();
            ensureColumn("answer_content",
                    "ALTER TABLE inquiries ADD COLUMN answer_content TEXT NULL");
            ensureColumn("answered_at",
                    "ALTER TABLE inquiries ADD COLUMN answered_at DATETIME(6) NULL");
            ensureColumn("updated_at",
                    "ALTER TABLE inquiries ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)");
            ensureIndex("idx_inquiries_account_id",
                    "CREATE INDEX idx_inquiries_account_id ON inquiries (account_id)");
            ensureIndex("idx_inquiries_status",
                    "CREATE INDEX idx_inquiries_status ON inquiries (status)");
            ensureIndex("idx_inquiries_type",
                    "CREATE INDEX idx_inquiries_type ON inquiries (type)");
            ensureIndex("idx_inquiries_created_at",
                    "CREATE INDEX idx_inquiries_created_at ON inquiries (created_at)");
        } catch (Exception e) {
            log.error("[inquiry] failed to ensure schema", e);
            throw e;
        }
    }

    private void ensureTable() {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'inquiries'
            """, Integer.class);

        if (count != null && count > 0) {
            return;
        }

        log.warn("[inquiry] missing table `inquiries`; applying schema patch");
        jdbcTemplate.execute("""
            CREATE TABLE inquiries (
                id BIGINT NOT NULL AUTO_INCREMENT,
                account_id BIGINT NOT NULL,
                type VARCHAR(30) NOT NULL,
                title VARCHAR(200) NOT NULL,
                content TEXT NOT NULL,
                status VARCHAR(30) NOT NULL,
                answer_content TEXT NULL,
                answered_at DATETIME(6) NULL,
                created_at DATETIME(6) NOT NULL,
                updated_at DATETIME(6) NOT NULL,
                PRIMARY KEY (id),
                CONSTRAINT fk_inquiries_account
                    FOREIGN KEY (account_id) REFERENCES account (id)
            )
            """);
        log.info("[inquiry] schema patch applied: created `inquiries`");
    }

    private void ensureColumn(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'inquiries'
              AND COLUMN_NAME = ?
            """, Integer.class, columnName);

        if (count != null && count > 0) {
            return;
        }

        log.warn("[inquiry] missing column `{}`; applying schema patch", columnName);
        jdbcTemplate.execute(ddl);
        log.info("[inquiry] schema patch applied: added `{}`", columnName);
    }

    private void ensureIndex(String indexName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'inquiries'
              AND INDEX_NAME = ?
            """, Integer.class, indexName);

        if (count != null && count > 0) {
            return;
        }

        log.warn("[inquiry] missing index `{}`; applying schema patch", indexName);
        jdbcTemplate.execute(ddl);
        log.info("[inquiry] schema patch applied: added `{}`", indexName);
    }
}
