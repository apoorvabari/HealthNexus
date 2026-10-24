package org.proj.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class ConsentTableConstraintFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            log.info("Checking patient_consent table unique constraints...");

            String query = """
                SELECT DISTINCT tc.CONSTRAINT_NAME
                FROM information_schema.TABLE_CONSTRAINTS tc
                WHERE tc.TABLE_SCHEMA = DATABASE()
                  AND tc.TABLE_NAME = 'patient_consent'
                  AND tc.CONSTRAINT_TYPE = 'UNIQUE'
            """;

            List<Map<String, Object>> constraints = jdbcTemplate.queryForList(query);
            for (Map<String, Object> c : constraints) {
                String constraintName = (String) c.get("CONSTRAINT_NAME");
                if (constraintName == null || constraintName.equalsIgnoreCase("PRIMARY")) {
                    continue;
                }

                String colQuery = """
                    SELECT COLUMN_NAME
                    FROM information_schema.KEY_COLUMN_USAGE
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'patient_consent'
                      AND CONSTRAINT_NAME = ?
                """;
                List<String> columns = jdbcTemplate.queryForList(colQuery, String.class, constraintName);
                if (columns.contains("patient_id") && columns.contains("consent_type") && !columns.contains("doctor_id")) {
                    log.info("Dropping obsolete unique constraint '{}' on columns {} from patient_consent", constraintName, columns);
                    try {
                        jdbcTemplate.execute("ALTER TABLE patient_consent DROP INDEX " + constraintName);
                        log.info("Successfully dropped index {}", constraintName);
                    } catch (Exception ex) {
                        try {
                            jdbcTemplate.execute("ALTER TABLE patient_consent DROP CONSTRAINT " + constraintName);
                            log.info("Successfully dropped constraint {}", constraintName);
                        } catch (Exception ex2) {
                            log.warn("Could not drop constraint {}: {}", constraintName, ex2.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Notice: consent constraint verification skipped: {}", e.getMessage());
        }
    }
}
