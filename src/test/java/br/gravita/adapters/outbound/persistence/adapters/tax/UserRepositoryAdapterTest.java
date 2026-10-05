package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.UserJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.UserPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.UserJpaRepository;
import br.gravita.adapters.outbound.security.PasswordHasher;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserNotFoundException;
import br.gravita.core.domain.system.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

	@Mock
	private UserJpaRepository repository;

	@Mock
	private PasswordHasher passwordHasher;

	@Mock
	private UserPersistenceMapper mapper;

	@InjectMocks
	private UserRepositoryAdapter adapter;

	@Test
	@DisplayName("Hashes the raw password before persisting the user")
	void shouldHashThePasswordBeforePersistingTheUser() {
		final User user = buildUser();
		final UserJpaEntity entity = buildEntity(user.getId().value());
		when(passwordHasher.hash("s3cret!")).thenReturn("$2a$10$hashed");
		when(mapper.map(user, "$2a$10$hashed")).thenReturn(entity);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity)).thenReturn(user);

		final User result = adapter.save(user);

		assertThat(result).isSameAs(user);
		verify(passwordHasher).hash("s3cret!");
		verify(mapper).map(user, "$2a$10$hashed");
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Updates a user without touching the password hash")
	void shouldUpdateUserWithoutTouchingThePasswordHash() {
		final User user = buildUser();
		final UserJpaEntity entity = buildEntity(user.getId().value());
		entity.setPasswordHash("original-hash");
		when(repository.findById(user.getId().value())).thenReturn(Optional.of(entity));

		adapter.update(user);

		assertThat(entity.getName()).isEqualTo(user.getName());
		assertThat(entity.getEmail()).isEqualTo(user.getEmail());
		assertThat(entity.getProfileId()).isEqualTo(user.getProfileId());
		assertThat(entity.isTwoFactorEnabled()).isEqualTo(user.isTwoFactorEnabled());
		assertThat(entity.getStatus()).isEqualTo(user.getStatus());
		assertThat(entity.getPasswordHash()).isEqualTo("original-hash");
		verify(repository).save(entity);
		verify(passwordHasher, never()).hash(any());
	}

	@Test
	@DisplayName("Updating the password stores only the hash of the new raw password")
	void shouldStoreOnlyTheHashOfTheNewPassword() {
		final User user = buildUser();
		user.changePassword("n3w-pass");
		final UserJpaEntity entity = buildEntity(user.getId().value());
		entity.setPasswordHash("original-hash");
		when(repository.findById(user.getId().value())).thenReturn(Optional.of(entity));
		when(passwordHasher.hash("n3w-pass")).thenReturn("$2a$10$new-hash");

		adapter.updatePassword(user);

		assertThat(entity.getPasswordHash()).isEqualTo("$2a$10$new-hash");
		assertThat(entity.getName()).isEqualTo("Old Name");
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Fails to update the password of an unknown user")
	void shouldFailToUpdateThePasswordOfAnUnknownUser() {
		final User user = buildUser();
		when(repository.findById(user.getId().value())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.updatePassword(user)).isInstanceOf(UserNotFoundException.class);
		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("Fails to update an unknown user")
	void shouldFailToUpdateAnUnknownUser() {
		final User user = buildUser();
		when(repository.findById(user.getId().value())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.update(user)).isInstanceOf(UserNotFoundException.class);
		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("Finds a user by id")
	void shouldFindUserById() {
		final User user = buildUser();
		final UserJpaEntity entity = buildEntity(user.getId().value());
		when(repository.findById(user.getId().value())).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(user);

		final Optional<User> result = adapter.findById(user.getId());

		assertThat(result).contains(user);
		verify(repository).findById(user.getId().value());
	}

	@Test
	@DisplayName("Returns empty when the user id is unknown")
	void shouldReturnEmptyWhenUserIdIsUnknown() {
		final UserId id = UserId.generate();
		when(repository.findById(id.value())).thenReturn(Optional.empty());

		assertThat(adapter.findById(id)).isEmpty();
	}

	@Test
	@DisplayName("Finds a user by email")
	void shouldFindUserByEmail() {
		final User user = buildUser();
		final UserJpaEntity entity = buildEntity(user.getId().value());
		when(repository.findByEmail("jane@example.com")).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(user);

		final Optional<User> result = adapter.findByEmail("jane@example.com");

		assertThat(result).contains(user);
		verify(repository).findByEmail("jane@example.com");
	}

	@Test
	@DisplayName("Reports whether an email is already taken")
	void shouldReportWhetherAnEmailIsTaken() {
		when(repository.existsByEmail("jane@example.com")).thenReturn(true);
		when(repository.existsByEmail("nobody@example.com")).thenReturn(false);

		assertThat(adapter.existsByEmail("jane@example.com")).isTrue();
		assertThat(adapter.existsByEmail("nobody@example.com")).isFalse();
	}

	@Test
	@DisplayName("Finds the users of a company through the company filter")
	void shouldFindUsersByCompanyId() {
		final UUID companyId = UUID.randomUUID();
		final User user = buildUser();
		final UserJpaEntity entity = buildEntity(user.getId().value());
		when(repository.findAllByCompanyId(companyId)).thenReturn(List.of(entity));
		when(mapper.map(entity)).thenReturn(user);

		assertThat(adapter.findAllByCompanyId(companyId)).containsExactly(user);
		verify(repository).findAllByCompanyId(companyId);
	}

	private UserJpaEntity buildEntity(final UUID id) {
		return UserJpaEntity.builder().id(id).name("Old Name").status(UserStatus.ACTIVE).build();
	}

	private User buildUser() {
		return User.register("Jane Doe", "jane@example.com", "s3cret!",
				new ProfileReference(UUID.randomUUID(), "Salesperson"));
	}
}
