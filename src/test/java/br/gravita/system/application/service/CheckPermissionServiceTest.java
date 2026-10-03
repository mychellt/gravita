package br.gravita.system.application.service;

import br.gravita.core.domain.PermissionAction;
import br.gravita.core.domain.PermissionDomain;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserStatus;
import br.gravita.core.ports.outbound.persistence.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.usercases.system.CheckPermissionQuery;
import br.gravita.core.usercases.tax.CheckPermissionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckPermissionServiceTest {

	@Mock
	private UserRepositoryPort userRepositoryPort;

	@Mock
	private ProfileRepositoryPort profileRepositoryPort;

	private CheckPermissionService service() {
		return new CheckPermissionService(userRepositoryPort, profileRepositoryPort);
	}

	private static final ProfileReference SOME_PROFILE = new ProfileReference(UUID.randomUUID(), "Salesperson");

	private User userWithProfile(UUID profileId) {
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SOME_PROFILE);
		user.update(null, null, new ProfileReference(profileId, "Salesperson"), null);
		return user;
	}

	@Test
	@DisplayName("Denies access when the user does not exist")
	void shouldDenyWhenUserDoesNotExist() {
		UserId userId = UserId.generate();
		when(userRepositoryPort.findById(userId)).thenReturn(Optional.empty());

		boolean result = service().execute(new CheckPermissionQuery(userId, "finance", "invoices", PermissionAction.VIEW));

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("Denies access when the user is inactive")
	void shouldDenyWhenUserIsInactive() {
		UUID profileId = UUID.randomUUID();
		User user = userWithProfile(profileId);
		user.update(null, null, null, UserStatus.INACTIVE);
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));

		boolean result = service().execute(new CheckPermissionQuery(user.getId(), "finance", "invoices", PermissionAction.VIEW));

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("Denies access when the user's profile no longer exists")
	void shouldDenyWhenProfileNoLongerExists() {
		UUID profileId = UUID.randomUUID();
		User user = userWithProfile(profileId);
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));
		when(profileRepositoryPort.findById(profileId)).thenReturn(Optional.empty());

		boolean result = service().execute(new CheckPermissionQuery(user.getId(), "finance", "invoices", PermissionAction.VIEW));

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("Denies access when the profile lacks the exact module, screen and action permission")
	void shouldDenyWhenProfileLacksTheExactModuleScreenActionTriple() {
		UUID profileId = UUID.randomUUID();
		User user = userWithProfile(profileId);
		ProfileDomain profile = ProfileDomain.builder().id(profileId).name("Salesperson")
				.permissions(List.of(
						PermissionDomain.builder().module("finance").screen("invoices").action(PermissionAction.VIEW).build(),
						PermissionDomain.builder().module("finance").screen("invoices").action(PermissionAction.EDIT).build()))
				.build();
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));
		when(profileRepositoryPort.findById(profileId)).thenReturn(Optional.of(profile));

		boolean result = service().execute(new CheckPermissionQuery(user.getId(), "finance", "invoices", PermissionAction.DELETE));

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("Allows access when a standard profile grants the exact module, screen and action")
	void shouldAllowWhenStandardProfileGrantsTheExactTriple() {
		UUID profileId = UUID.randomUUID();
		User user = userWithProfile(profileId);
		ProfileDomain profile = ProfileDomain.builder().id(profileId).name("Salesperson")
				.permissions(List.of(PermissionDomain.builder().module("finance").screen("invoices").action(PermissionAction.VIEW).build()))
				.build();
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));
		when(profileRepositoryPort.findById(profileId)).thenReturn(Optional.of(profile));

		boolean result = service().execute(new CheckPermissionQuery(user.getId(), "finance", "invoices", PermissionAction.VIEW));

		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("Allows access when a custom profile grants the exact module, screen and action")
	void shouldAllowWhenCustomProfileGrantsTheExactTriple() {
		UUID profileId = UUID.randomUUID();
		User user = userWithProfile(profileId);
		ProfileDomain customProfile = ProfileDomain.builder().id(profileId).name("Regional Approver")
				.permissions(List.of(PermissionDomain.builder().module("sales").screen("orders").action(PermissionAction.APPROVE).build()))
				.build();
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));
		when(profileRepositoryPort.findById(profileId)).thenReturn(Optional.of(customProfile));

		boolean result = service().execute(new CheckPermissionQuery(user.getId(), "sales", "orders", PermissionAction.APPROVE));

		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("Denies access when the profile has no permissions at all")
	void shouldDenyWhenProfileHasNoPermissionsAtAll() {
		UUID profileId = UUID.randomUUID();
		User user = userWithProfile(profileId);
		ProfileDomain profile = ProfileDomain.builder().id(profileId).name("Empty").permissions(null).build();
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));
		when(profileRepositoryPort.findById(profileId)).thenReturn(Optional.of(profile));

		boolean result = service().execute(new CheckPermissionQuery(user.getId(), "finance", "invoices", PermissionAction.VIEW));

		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("Denies access to a user pending activation (no change to the service needed)")
	void shouldDenyWhenUserIsPendingActivation() {
		User user = User.signUp("Jane Doe", "jane@example.com", "s3cret!", SOME_PROFILE, UUID.randomUUID());
		when(userRepositoryPort.findById(user.getId())).thenReturn(Optional.of(user));

		boolean result = service().execute(
				new CheckPermissionQuery(user.getId(), "finance", "invoices", PermissionAction.VIEW));

		assertThat(result).isFalse();
	}
}
