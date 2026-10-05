package br.gravita.core.usercases.system;

import br.gravita.core.domain.exceptions.UnauthorizedException;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserStatus;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.usercases.CallerCompanyResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class ListUsersServiceTest {

	private static final UUID COMPANY_ID = UUID.randomUUID();
	private static final UUID OTHER_COMPANY_ID = UUID.randomUUID();
	private static final ProfileReference ADMIN = new ProfileReference(UUID.randomUUID(), "Administrator");
	private static final ProfileReference SALESPERSON = new ProfileReference(UUID.randomUUID(), "Salesperson");

	@Mock
	private UserRepositoryPort userRepositoryPort;

	@Mock
	private ProfileRepositoryPort profileRepositoryPort;

	private ListUsersService service() {
		return new ListUsersService(userRepositoryPort, profileRepositoryPort, new CallerCompanyResolver(userRepositoryPort));
	}

	@Test
	@DisplayName("Lists only the users of the caller's company, never those of another company")
	void shouldListOnlyTheUsersOfTheCallersCompany() {
		User caller = User.register("Tenant Admin", "admin@acme.com", "s3cret!", ADMIN, COMPANY_ID);
		User colleague = User.register("Jane Doe", "jane@acme.com", "s3cret!", SALESPERSON, COMPANY_ID);
		when(userRepositoryPort.findById(caller.getId())).thenReturn(Optional.of(caller));
		when(userRepositoryPort.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(caller, colleague));
		when(profileRepositoryPort.findAll()).thenReturn(List.of(ADMIN, SALESPERSON));

		List<UserSummary> result = service().execute(caller.getId());

		assertThat(result).extracting(UserSummary::email).containsExactly("admin@acme.com", "jane@acme.com");
		verify(userRepositoryPort).findAllByCompanyId(COMPANY_ID);
		verify(userRepositoryPort, never()).findAllByCompanyId(OTHER_COMPANY_ID);
	}

	@Test
	@DisplayName("Summarises id, name, email, profile, two-factor and status without exposing the password")
	void shouldSummariseEachUser() {
		User caller = User.register("Tenant Admin", "admin@acme.com", "s3cret!", ADMIN, COMPANY_ID);
		User inactive = User.register("Jane Doe", "jane@acme.com", "s3cret!", SALESPERSON, COMPANY_ID);
		inactive.update(null, null, null, UserStatus.INACTIVE);
		when(userRepositoryPort.findById(caller.getId())).thenReturn(Optional.of(caller));
		when(userRepositoryPort.findAllByCompanyId(COMPANY_ID)).thenReturn(List.of(caller, inactive));
		when(profileRepositoryPort.findAll()).thenReturn(List.of(ADMIN, SALESPERSON));

		List<UserSummary> result = service().execute(caller.getId());

		assertThat(result).containsExactly(
				new UserSummary(caller.getId().value(), "Tenant Admin", "admin@acme.com", ADMIN.id(), "Administrator",
						true, UserStatus.ACTIVE),
				new UserSummary(inactive.getId().value(), "Jane Doe", "jane@acme.com", SALESPERSON.id(), "Salesperson",
						false, UserStatus.INACTIVE));
	}

	@Test
	@DisplayName("Returns an empty list without querying users when the caller has no company")
	void shouldReturnEmptyWhenTheCallerHasNoCompany() {
		User orphan = User.register("Legacy Admin", "legacy@example.com", "s3cret!", ADMIN);
		when(userRepositoryPort.findById(orphan.getId())).thenReturn(Optional.of(orphan));

		assertThat(service().execute(orphan.getId())).isEmpty();

		verify(userRepositoryPort, never()).findAllByCompanyId(any());
	}

	@Test
	@DisplayName("Requires an authenticated caller")
	void shouldRequireAnAuthenticatedCaller() {
		assertThatThrownBy(() -> service().execute(null)).isInstanceOf(UnauthorizedException.class);

		verify(userRepositoryPort, never()).findAllByCompanyId(any());
	}
}
