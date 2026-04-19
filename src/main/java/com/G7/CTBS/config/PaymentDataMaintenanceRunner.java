package com.G7.CTBS.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentDataMaintenanceRunner implements CommandLineRunner {

    private static final String PAYMENTS_TABLE_EXISTS_SQL = """
            SELECT COUNT(1)
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = 'payments'
            """;

    private static final String DEDUPLICATE_SQL = """
            WITH ranked AS (
                SELECT
                    payment_id,
                    ROW_NUMBER() OVER (
                        PARTITION BY transaction_ref
                        ORDER BY
                            CASE payment_status
                                WHEN 'SUCCESS' THEN 3
                                WHEN 'PROCESSING' THEN 2
                                WHEN 'PENDING' THEN 1
                                ELSE 0
                            END DESC,
                            COALESCE(payment_time, created_at) DESC,
                            payment_id DESC
                    ) AS rn
                FROM dbo.payments
                WHERE transaction_ref IS NOT NULL
            )
            DELETE p
            FROM dbo.payments p
            JOIN ranked r ON p.payment_id = r.payment_id
            WHERE r.rn > 1
            """;

    private static final String CREATE_UNIQUE_INDEX_SQL = """
            IF NOT EXISTS (
                SELECT 1
                FROM sys.indexes
                WHERE name = 'UX_payments_transaction_ref'
                  AND object_id = OBJECT_ID('dbo.payments')
            )
            BEGIN
                CREATE UNIQUE INDEX UX_payments_transaction_ref
                ON dbo.payments(transaction_ref)
                WHERE transaction_ref IS NOT NULL
            END
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        Integer paymentsTableExists = jdbcTemplate.queryForObject(PAYMENTS_TABLE_EXISTS_SQL, Integer.class);
        if (paymentsTableExists == null || paymentsTableExists == 0) {
            log.info("Skip payment maintenance because dbo.payments does not exist.");
            return;
        }

        int removedDuplicates = jdbcTemplate.update(DEDUPLICATE_SQL);
        if (removedDuplicates > 0) {
            log.warn("Payment maintenance removed {} duplicate payment rows by transaction_ref.", removedDuplicates);
        } else {
            log.info("Payment maintenance found no duplicate transaction_ref rows.");
        }

        jdbcTemplate.execute(CREATE_UNIQUE_INDEX_SQL);
        log.info("Payment maintenance ensured unique index UX_payments_transaction_ref.");
    }
}
