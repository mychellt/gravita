package br.gravita.system.adapter.out.persistence;

import br.gravita.system.domain.model.ProfileReference;
import br.gravita.system.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({UserRepositoryAdapter.class, PasswordHasher.class})
class UserRepositoryAdapterTest {

	@Autowired
	private UserRepositoryAdapter repositoryAdapter;

	@Autowired
	private UserJpaRepository jpaRepository;

	@Test
	void shouldPersistUserAndReportEmailAsTaken() {
		ProfileReference salesperson = new ProfileReference(UUID.randomUUID(), "Salesperson");
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", salesperson);

		repositoryAdapter.save(user);

		assertThat(repositoryAdapter.existsByEmail("jane@example.com")).isTrue();
		assertThat(repositoryAdapter.existsByEmail("nobody@example.com")).isFalse();
	}

	@Test
	void shouldNeverPersistThePlaintextPassword() {
		ProfileReference salesperson = new ProfileReference(UUID.randomUUID(), "Salesperson");
		User user = User.register("Jane Doe", "jane@example.com", "plain-text-password", salesperson);

		repositoryAdapter.save(user);

		UserJpaEntity stored = jpaRepository.findAll().get(0);
		assertThat(stored.getPasswordHash()).doesNotContain("plain-text-password");
		assertThat(stored.getPasswordHash()).startsWith("$2");
	}

	@Test
	void shouldForceTwoFactorEnabledWhenProfileIsAdministrator() {
		ProfileReference administrator = new ProfileReference(UUID.randomUUID(), "Administrator");
		User user = User.register("Admin User", "admin@example.com", "s3cret!", administrator);

		repositoryAdapter.save(user);

		UserJpaEntity stored = jpaRepository.findAll().get(0);
		assertThat(stored.isTwoFactorEnabled()).isTrue();
	}
}
