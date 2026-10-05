package br.gravita.system.application.service;

import br.gravita.core.usercases.tax.UpdateUserService;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.usercases.system.UpdateUserCommand;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.UnknownProfileException;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserNotFoundException;
import br.gravita.core.domain.system.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateUserServiceTest {

	@Mock
	private UserRepositoryPort userRepositoryPort;

	@Mock
	private ProfileRepositoryPort profileRepositoryPort;

	private static final ProfileReference SALESPERSON = new ProfileReference(UUID.randomUUID(), "Salesperson");
	private static final ProfileReference ADMINISTRATOR = new ProfileReference(UUID.randomUUID(), "Administrator");

	@Test
	@DisplayName("Updates name, email, profile and status")
	void shouldUpdateNameEmailProfileAndStatus() {
		final UpdateUserService service = new UpdateUserService(userRepositoryPort, profileRepositoryPort);
		final User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));
		when(userRepositoryPort.existsByEmail("jane.roe@example.com")).thenReturn(false);
		when(profileRepositoryPort.findById(ADMINISTRATOR.id())).thenReturn(Optional.of(ADMINISTRATOR));

		service.execute(new UpdateUserCommand(user.getId().value(), "Jane Roe", "jane.roe@example.com",
				ADMINISTRATOR.id(), UserStatus.INACTIVE));

		final ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepositoryPort).update(captor.capture());
		final User updated = captor.getValue();
		assertThat(updated.getName()).isEqualTo("Jane Roe");
		assertThat(updated.getEmail()).isEqualTo("jane.roe@example.com");
		assertThat(updated.getProfileId()).isEqualTo(ADMINISTRATOR.id());
		assertThat(updated.getStatus()).isEqualTo(UserStatus.INACTIVE);
	}

	@Test
	@DisplayName("Forces two-factor on when the profile switches to administrator")
	void shouldForceTwoFactorEnabledWhenProfileSwitchesToAdministrator() {
		final UpdateUserService service = new UpdateUserService(userRepositoryPort, profileRepositoryPort);
		final User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));
		when(profileRepositoryPort.findById(ADMINISTRATOR.id())).thenReturn(Optional.of(ADMINISTRATOR));

		service.execute(new UpdateUserCommand(user.getId().value(), null, null, ADMINISTRATOR.id(), null));

		final ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepositoryPort).update(captor.capture());
		assertThat(captor.getValue().isTwoFactorEnabled()).isTrue();
	}

	@Test
	@DisplayName("Rejects an unknown user id")
	void shouldRejectUnknownUserId() {
		final UpdateUserService service = new UpdateUserService(userRepositoryPort, profileRepositoryPort);
		final UUID userId = UUID.randomUUID();
		when(userRepositoryPort.findById(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new UpdateUserCommand(userId, "Jane Roe", null, null, null)))
				.isInstanceOf(UserNotFoundException.class);

		verify(userRepositoryPort, never()).update(any());
	}

	@Test
	@DisplayName("Rejects an email already used by another user on update")
	void shouldRejectDuplicateEmailOnUpdate() {
		final UpdateUserService service = new UpdateUserService(userRepositoryPort, profileRepositoryPort);
		final User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));
		when(userRepositoryPort.existsByEmail("taken@example.com")).thenReturn(true);

		assertThatThrownBy(() -> service.execute(
				new UpdateUserCommand(user.getId().value(), null, "taken@example.com", null, null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("taken@example.com");

		verify(userRepositoryPort, never()).update(any());
	}

	@Test
	@DisplayName("Does not treat an unchanged email as a duplicate")
	void shouldNotTreatUnchangedEmailAsDuplicate() {
		final UpdateUserService service = new UpdateUserService(userRepositoryPort, profileRepositoryPort);
		final User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));

		service.execute(new UpdateUserCommand(user.getId().value(), "Jane Roe", "jane@example.com", null, null));

		verify(userRepositoryPort, never()).existsByEmail(any());
		verify(userRepositoryPort).update(any());
	}

	@Test
	@DisplayName("Rejects an unknown profile on update")
	void shouldRejectUnknownProfileOnUpdate() {
		final UpdateUserService service = new UpdateUserService(userRepositoryPort, profileRepositoryPort);
		final User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);
		final UUID unknownProfileId = UUID.randomUUID();
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));
		when(profileRepositoryPort.findById(unknownProfileId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(
				new UpdateUserCommand(user.getId().value(), null, null, unknownProfileId, null)))
				.isInstanceOf(UnknownProfileException.class);

		verify(userRepositoryPort, never()).update(any());
	}
}
