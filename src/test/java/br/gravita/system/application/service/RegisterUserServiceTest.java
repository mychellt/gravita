package br.gravita.system.application.service;

import br.gravita.core.usercases.tax.RegisterUserService;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.usercases.system.RegisterUserCommand;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.UnknownProfileException;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
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
class RegisterUserServiceTest {

	@Mock
	private UserRepositoryPort userRepositoryPort;

	@Mock
	private ProfileRepositoryPort profileRepositoryPort;

	@Test
	void shouldRegisterUserWhenEmailIsFreeAndProfileExists() {
		RegisterUserService service = new RegisterUserService(userRepositoryPort, profileRepositoryPort);
		UUID profileId = UUID.randomUUID();
		when(userRepositoryPort.existsByEmail("jane@example.com")).thenReturn(false);
		when(profileRepositoryPort.findById(profileId))
				.thenReturn(Optional.of(new ProfileReference(profileId, "Salesperson")));
		when(userRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		UserId result = service.execute(new RegisterUserCommand("Jane Doe", "jane@example.com", "s3cret!", profileId));

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getEmail()).isEqualTo("jane@example.com");
		assertThat(captor.getValue().isTwoFactorEnabled()).isFalse();
		assertThat(result).isEqualTo(captor.getValue().getId());
	}

	@Test
	void shouldForceTwoFactorEnabledWhenProfileIsAdministrator() {
		RegisterUserService service = new RegisterUserService(userRepositoryPort, profileRepositoryPort);
		UUID profileId = UUID.randomUUID();
		when(userRepositoryPort.existsByEmail("admin@example.com")).thenReturn(false);
		when(profileRepositoryPort.findById(profileId))
				.thenReturn(Optional.of(new ProfileReference(profileId, "Administrator")));
		when(userRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new RegisterUserCommand("Admin User", "admin@example.com", "s3cret!", profileId));

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().isTwoFactorEnabled()).isTrue();
	}

	@Test
	void shouldRejectDuplicateEmailBeforeTouchingTheProfile() {
		RegisterUserService service = new RegisterUserService(userRepositoryPort, profileRepositoryPort);
		when(userRepositoryPort.existsByEmail("jane@example.com")).thenReturn(true);

		assertThatThrownBy(() -> service.execute(
				new RegisterUserCommand("Jane Doe", "jane@example.com", "s3cret!", UUID.randomUUID())))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("jane@example.com");

		verify(profileRepositoryPort, never()).findById(any());
		verify(userRepositoryPort, never()).save(any());
	}

	@Test
	void shouldRejectUnknownProfile() {
		RegisterUserService service = new RegisterUserService(userRepositoryPort, profileRepositoryPort);
		UUID profileId = UUID.randomUUID();
		when(userRepositoryPort.existsByEmail("jane@example.com")).thenReturn(false);
		when(profileRepositoryPort.findById(profileId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(
				new RegisterUserCommand("Jane Doe", "jane@example.com", "s3cret!", profileId)))
				.isInstanceOf(UnknownProfileException.class);

		verify(userRepositoryPort, never()).save(any());
	}
}
