package br.gravita.adapters.configuration.persistence;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Loads the default plan catalog on startup while Flyway is disabled and Hibernate's {@code ddl-auto=update}
 * manages the schema, so a fresh local database still has Bronze, Silver and Gold. It runs the very script
 * Flyway applies once migrations are enabled, which only seeds a catalog that is still empty.
 */
@Component
@ConditionalOnProperty(name = "gravita.plans.seed-defaults-when-empty", havingValue = "true")
public class DefaultPlansSeeder implements ApplicationRunner {

	private static final String SEED_SCRIPT = "db/migration/local/V77__seed_default_plans.sql";

	private final DataSource dataSource;

	public DefaultPlansSeeder(final DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public void run(final ApplicationArguments args) {
		new ResourceDatabasePopulator(new ClassPathResource(SEED_SCRIPT)).execute(dataSource);
	}
}
