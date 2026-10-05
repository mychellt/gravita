package br.gravita.system.application.service;

import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.usercases.CallerCompanyResolver;
import br.gravita.core.usercases.tax.RegisterUserService;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.usercases.system.RegisterUserCommand;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.UnknownProfileException;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
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
class RegisterUserServiceTest {

	@Mock
	private UserRepositoryPort userRepositoryPort;

	@Mock
	private ProfileRepositoryPort profileRepositoryPort;

	private static final UUID COMPANY_ID = UUID.randomUUID();
	private static final ProfileReference ADMIN = new ProfileReference(UUID.randomUUID(), "Administrator");

	private final User caller = User.register("Tenant Admin", "admin@acme.com", "s3cret!", ADMIN, COMPANY_ID);

	private RegisterUserService service() {
		return new RegisterUserService(userRepositoryPort, profileRepositoryPort,
				new CallerCompanyResolver(userRepositoryPort));
	}

	private RegisterUserCommand command(String name, String email, UUID profileId) {
		return new RegisterUserCommand(name, email, "s3cret!", profileId, caller.getId());
	}

	@Test
	@DisplayName("Registers a user when the email is free and the profile exists")
	void shouldRegisterUserWhenEmailIsFreeAndProfileExists() {
		RegisterUserService service = service();
		when(userRepositoryPort.findById(caller.getId())).thenReturn(Optional.of(caller));
		UUID profileId = UUID.randomUUID();
		when(userRepositoryPort.existsByEmail("jane@example.com")).thenReturn(false);
		when(profileRepositoryPort.findById(profileId))
				.thenReturn(Optional.of(new ProfileReference(profileId, "Salesperson")));
		when(userRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		UserId result = service.execute(command("Jane Doe", "jane@example.com", profileId));

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getEmail()).isEqualTo("jane@example.com");
		assertThat(captor.getValue().isTwoFactorEnabled()).isFalse();
		assertThat(result).isEqualTo(captor.getValue().getId());
	}

	@Test
	@DisplayName("Forces two-factor on when the profile is administrator")
	void shouldForceTwoFactorEnabledWhenProfileIsAdministrator() {
		RegisterUserService service = service();
		when(userRepositoryPort.findById(caller.getId())).thenReturn(Optional.of(caller));
		UUID profileId = UUID.randomUUID();
		when(userRepositoryPort.existsByEmail("admin@example.com")).thenReturn(false);
		when(profileRepositoryPort.findById(profileId))
				.thenReturn(Optional.of(new ProfileReference(profileId, "Administrator")));
		when(userRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(command("Admin User", "admin@example.com", profileId));

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().isTwoFactorEnabled()).isTrue();
	}

	@Test
	@DisplayName("Rejects a duplicate email before touching the profile")
	void shouldRejectDuplicateEmailBeforeTouchingTheProfile() {
		RegisterUserService service = service();
		when(userRepositoryPort.findById(caller.getId())).thenReturn(Optional.of(caller));
		when(userRepositoryPort.existsByEmail("jane@example.com")).thenReturn(true);

		assertThatThrownBy(() -> service.execute(
				command("Jane Doe", "jane@example.com", UUID.randomUUID())))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("jane@example.com");

		verify(profileRepositoryPort, never()).findById(any());
		verify(userRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects an unknown profile")
	void shouldRejectUnknownProfile() {
		RegisterUserService service = service();
		when(userRepositoryPort.findById(caller.getId())).thenReturn(Optional.of(caller));
		UUID profileId = UUID.randomUUID();
		when(userRepositoryPort.existsByEmail("jane@example.com")).thenReturn(false);
		when(profileRepositoryPort.findById(profileId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(
				command("Jane Doe", "jane@example.com", profileId)))
				.isInstanceOf(UnknownProfileException.class);

		verify(userRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Stamps the new user with the company of the authenticated caller")
	void shouldStampTheNewUserWithTheCallersCompany() {
		RegisterUserService service = service();
		UUID profileId = UUID.randomUUID();
		when(userRepositoryPort.findById(caller.getId())).thenReturn(Optional.of(caller));
		when(userRepositoryPort.existsByEmail("jane@example.com")).thenReturn(false);
		when(profileRepositoryPort.findById(profileId))
				.thenReturn(Optional.of(new ProfileReference(profileId, "Salesperson")));
		when(userRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(command("Jane Doe", "jane@example.com", profileId));

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getCompanyId()).isEqualTo(COMPANY_ID);
	}

	@Test
	@DisplayName("Refuses to register a user when the caller does not belong to a company")
	void shouldRefuseToRegisterWhenTheCallerHasNoCompany() {
		RegisterUserService service = service();
		User orphanCaller = User.register("Legacy Admin", "legacy@example.com", "s3cret!", ADMIN);
		when(userRepositoryPort.findById(orphanCaller.getId())).thenReturn(Optional.of(orphanCaller));

		assertThatThrownBy(() -> service.execute(new RegisterUserCommand("Jane Doe", "jane@example.com", "s3cret!",
				UUID.randomUUID(), orphanCaller.getId())))
				.isInstanceOf(ForbiddenException.class);

		verify(userRepositoryPort, never()).save(any());
	}
}
