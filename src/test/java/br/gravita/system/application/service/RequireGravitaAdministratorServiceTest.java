package br.gravita.system.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.usercases.system.RequireGravitaAdministratorService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequireGravitaAdministratorServiceTest {

	private final UserRepositoryPort users = mock(UserRepositoryPort.class);
	private final ProfileRepositoryPort profiles = mock(ProfileRepositoryPort.class);
	private final RequireGravitaAdministratorService service = new RequireGravitaAdministratorService(users, profiles);

	private UserId userWithProfile(final String profileName) {
		final UserId userId = UserId.generate();
		final UUID profileId = UUID.randomUUID();
		final User user = mock(User.class);
		when(user.getProfileId()).thenReturn(profileId);
		when(users.findById(userId)).thenReturn(Optional.of(user));
		when(profiles.findById(profileId)).thenReturn(Optional.of(new ProfileReference(profileId, profileName)));
		return userId;
	}

	@Test
	@DisplayName("lets a GRAVITA_ADMINISTRATOR through")
	void allowsGravitaAdministrator() {
		final UserId userId = userWithProfile("GRAVITA_ADMINISTRATOR");

		assertThatCode(() -> service.execute(userId)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("refuses a tenant Administrator and any other profile")
	void refusesOtherProfiles() {
		final UserId tenantAdmin = userWithProfile("Administrator");
		final UserId cashier = userWithProfile("Cashier Operator");

		assertThatThrownBy(() -> service.execute(tenantAdmin)).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> service.execute(cashier)).isInstanceOf(ForbiddenException.class);
	}

	@Test
	@DisplayName("refuses an unknown user")
	void refusesUnknownUser() {
		when(users.findById(UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000001")))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"))))
				.isInstanceOf(ForbiddenException.class);
	}
}
