package com.koda.levi.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Aiven's free MySQL plan auto-pauses after a stretch with no queries against it (see
// levi/DEPLOYMENT.md). A trivial periodic query keeps it counted as active. Note this only
// helps while this instance itself is running — Render's own free web service separately
// sleeps after ~15 min with no *incoming HTTP* traffic (internal scheduled tasks don't count
// as activity there), so an external ping hitting a public endpoint is still needed to keep
// Render itself awake; this alone doesn't cover that half of the problem.
@Component
public class DatabaseKeepAliveScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseKeepAliveScheduler.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseKeepAliveScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void pingDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            logger.debug("Database keep-alive ping succeeded");
        } catch (Exception e) {
            logger.warn("Database keep-alive ping failed: {}", e.getMessage());
        }
    }
}
