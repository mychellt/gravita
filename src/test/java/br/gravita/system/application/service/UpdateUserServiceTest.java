package br.gravita.system.application.service;

import br.gravita.shared.BusinessRuleException;
import br.gravita.system.application.port.in.UpdateUserCommand;
import br.gravita.system.application.port.out.ProfileRepositoryPort;
import br.gravita.system.application.port.out.UserRepositoryPort;
import br.gravita.system.domain.model.ProfileReference;
import br.gravita.system.domain.model.UnknownProfileException;
import br.gravita.system.domain.model.User;
import br.gravita.system.domain.model.UserNotFoundException;
import br.gravita.system.domain.model.UserStatus;
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
	void shouldUpdateNameEmailProfileAndStatus() {
		UpdateUserService service = new UpdateUserService(userRepositoryPort, profileRepositoryPort);
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));
		when(userRepositoryPort.existsByEmail("jane.roe@example.com")).thenReturn(false);
		when(profileRepositoryPort.findById(ADMINISTRATOR.id())).thenReturn(Optional.of(ADMINISTRATOR));

		service.execute(new UpdateUserCommand(user.getId().value(), "Jane Roe", "jane.roe@example.com",
				ADMINISTRATOR.id(), UserStatus.INACTIVE));

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepositoryPort).update(captor.capture());
		User updated = captor.getValue();
		assertThat(updated.getName()).isEqualTo("Jane Roe");
		assertThat(updated.getEmail()).isEqualTo("jane.roe@example.com");
		assertThat(updated.getProfileId()).isEqualTo(ADMINISTRATOR.id());
		assertThat(updated.getStatus()).isEqualTo(UserStatus.INACTIVE);
	}

	@Test
	void shouldForceTwoFactorEnabledWhenProfileSwitchesToAdministrator() {
		UpdateUserService service = new UpdateUserService(userRepositoryPort, profileRepositoryPort);
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));
		when(profileRepositoryPort.findById(ADMINISTRATOR.id())).thenReturn(Optional.of(ADMINISTRATOR));

		service.execute(new UpdateUserCommand(user.getId().value(), null, null, ADMINISTRATOR.id(), null));

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepositoryPort).update(captor.capture());
		assertThat(captor.getValue().isTwoFactorEnabled()).isTrue();
	}

	@Test
	void shouldRejectUnknownUserId() {
		UpdateUserService service = new UpdateUserService(userRepositoryPort, profileRepositoryPort);
		UUID userId = UUID.randomUUID();
		when(userRepositoryPort.findById(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new UpdateUserCommand(userId, "Jane Roe", null, null, null)))
				.isInstanceOf(UserNotFoundException.class);

		verify(userRepositoryPort, never()).update(any());
	}

	@Test
	void shouldRejectDuplicateEmailOnUpdate() {
		UpdateUserService service = new UpdateUserService(userRepositoryPort, profileRepositoryPort);
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));
		when(userRepositoryPort.existsByEmail("taken@example.com")).thenReturn(true);

		assertThatThrownBy(() -> service.execute(
				new UpdateUserCommand(user.getId().value(), null, "taken@example.com", null, null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("taken@example.com");

		verify(userRepositoryPort, never()).update(any());
	}

	@Test
	void shouldNotTreatUnchangedEmailAsDuplicate() {
		UpdateUserService service = new UpdateUserService(userRepositoryPort, profileRepositoryPort);
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));

		service.execute(new UpdateUserCommand(user.getId().value(), "Jane Roe", "jane@example.com", null, null));

		verify(userRepositoryPort, never()).existsByEmail(any());
		verify(userRepositoryPort).update(any());
	}

	@Test
	void shouldRejectUnknownProfileOnUpdate() {
		UpdateUserService service = new UpdateUserService(userRepositoryPort, profileRepositoryPort);
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);
		UUID unknownProfileId = UUID.randomUUID();
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));
		when(profileRepositoryPort.findById(unknownProfileId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(
				new UpdateUserCommand(user.getId().value(), null, null, unknownProfileId, null)))
				.isInstanceOf(UnknownProfileException.class);

		verify(userRepositoryPort, never()).update(any());
	}
}
