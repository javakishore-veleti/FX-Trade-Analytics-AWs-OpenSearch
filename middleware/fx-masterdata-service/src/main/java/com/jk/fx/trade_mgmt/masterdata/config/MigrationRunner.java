package com.jk.fx.trade_mgmt.masterdata.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * One-shot Liquibase runner. Activated by {@code --spring.profiles.active=migrate}.
 *
 * <p>Spring Boot's {@code LiquibaseAutoConfiguration} runs the changelog as part
 * of context initialisation — by the time this {@code ApplicationRunner.run}
 * fires, all pending migrations have already been applied (or the boot would
 * have failed). We then exit so Kubernetes can mark the migration Job as
 * complete.
 *
 * <p>Pair with {@code spring.main.web-application-type: none} (set in the
 * {@code migrate} profile) so no servlet container or other long-lived bean
 * keeps the JVM alive.
 *
 * <p>Pod runtime profile (production): use {@code postgres,no-migrate} —
 * Liquibase is then disabled in the pod, the schema is whatever the prior
 * Job + the {@code DATABASECHANGELOG} table say it is, and Hibernate's
 * {@code ddl-auto: validate} catches any drift at boot.
 */
@Slf4j
@Component
@Profile("migrate")
@RequiredArgsConstructor
public class MigrationRunner implements ApplicationRunner {

    private final ConfigurableApplicationContext ctx;

    @Override
    public void run(ApplicationArguments args) {
        log.info("MigrationRunner: Liquibase has finished — exiting with code 0.");
        int exitCode = SpringApplication.exit(ctx, () -> 0);
        // SpringApplication.exit triggers shutdown hooks but doesn't terminate the JVM
        // when web-application-type=none has no daemon threads keeping it alive. Belt
        // and braces: explicitly System.exit so the K8s Job's container terminates.
        System.exit(exitCode);
    }
}
