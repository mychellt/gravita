package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.UserJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.UserPersistenceMapperImpl;
import br.gravita.adapters.outbound.persistence.repositories.tax.UserJpaRepository;
import br.gravita.adapters.outbound.security.PasswordHasher;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserNotFoundException;
import br.gravita.core.domain.system.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({UserRepositoryAdapter.class, PasswordHasher.class, UserPersistenceMapperImpl.class})
class UserRepositoryAdapterTest {

	@Autowired
	private UserRepositoryAdapter repositoryAdapter;

	@Autowired
	private UserJpaRepository jpaRepository;

	@Test
	@DisplayName("Persists a user and reports its email as taken")
	void shouldPersistUserAndReportEmailAsTaken() {
		ProfileReference salesperson = new ProfileReference(UUID.randomUUID(), "Salesperson");
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", salesperson);

		repositoryAdapter.save(user);

		assertThat(repositoryAdapter.existsByEmail("jane@example.com")).isTrue();
		assertThat(repositoryAdapter.existsByEmail("nobody@example.com")).isFalse();
	}

	@Test
	@DisplayName("Never persists the plaintext password")
	void shouldNeverPersistThePlaintextPassword() {
		ProfileReference salesperson = new ProfileReference(UUID.randomUUID(), "Salesperson");
		User user = User.register("Jane Doe", "jane@example.com", "plain-text-password", salesperson);

		repositoryAdapter.save(user);

		UserJpaEntity stored = jpaRepository.findAll().get(0);
		assertThat(stored.getPasswordHash()).doesNotContain("plain-text-password");
		assertThat(stored.getPasswordHash()).startsWith("$2");
	}

	@Test
	@DisplayName("Forces two-factor authentication on when the profile is Administrator")
	void shouldForceTwoFactorEnabledWhenProfileIsAdministrator() {
		ProfileReference administrator = new ProfileReference(UUID.randomUUID(), "Administrator");
		User user = User.register("Admin User", "admin@example.com", "s3cret!", administrator);

		repositoryAdapter.save(user);

		UserJpaEntity stored = jpaRepository.findAll().get(0);
		assertThat(stored.isTwoFactorEnabled()).isTrue();
	}

	@Test
	@DisplayName("Finds a persisted user by id")
	void shouldFindPersistedUserById() {
		ProfileReference salesperson = new ProfileReference(UUID.randomUUID(), "Salesperson");
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", salesperson);
		repositoryAdapter.save(user);

		Optional<User> found = repositoryAdapter.findById(user.getId());

		assertThat(found).isPresent();
		assertThat(found.get().getEmail()).isEqualTo("jane@example.com");
		assertThat(found.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("Returns empty when the user id is unknown")
	void shouldReturnEmptyWhenUserIdIsUnknown() {
		assertThat(repositoryAdapter.findById(UserId.generate())).isEmpty();
	}

	@Test
	@DisplayName("Updates a user without touching the password hash")
	void shouldUpdateUserWithoutTouchingThePasswordHash() {
		ProfileReference salesperson = new ProfileReference(UUID.randomUUID(), "Salesperson");
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", salesperson);
		repositoryAdapter.save(user);
		String originalHash = jpaRepository.findAll().get(0).getPasswordHash();

		User loaded = repositoryAdapter.findById(user.getId()).orElseThrow();
		loaded.update("Jane Roe", null, null, UserStatus.INACTIVE);
		repositoryAdapter.update(loaded);

		UserJpaEntity stored = jpaRepository.findAll().get(0);
		assertThat(stored.getName()).isEqualTo("Jane Roe");
		assertThat(stored.getStatus()).isEqualTo(UserStatus.INACTIVE);
		assertThat(stored.getPasswordHash()).isEqualTo(originalHash);
	}

	@Test
	@DisplayName("Fails to update an unknown user")
	void shouldFailToUpdateAnUnknownUser() {
		ProfileReference salesperson = new ProfileReference(UUID.randomUUID(), "Salesperson");
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", salesperson);

		assertThatThrownBy(() -> repositoryAdapter.update(user)).isInstanceOf(UserNotFoundException.class);
	}
}
