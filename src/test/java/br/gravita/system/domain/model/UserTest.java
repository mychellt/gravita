package br.gravita.system.domain.model;

import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

	private static final ProfileReference SALESPERSON = new ProfileReference(UUID.randomUUID(), "Salesperson");
	private static final ProfileReference ADMINISTRATOR = new ProfileReference(UUID.randomUUID(), "Administrator");

	@Test
	@DisplayName("Registers a user with a generated id and two-factor disabled for a non-admin profile")
	void shouldRegisterUserWithGeneratedIdAndTwoFactorDisabledForNonAdminProfile() {
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);

		assertThat(user.getId()).isNotNull();
		assertThat(user.getName()).isEqualTo("Jane Doe");
		assertThat(user.getEmail()).isEqualTo("jane@example.com");
		assertThat(user.getRawPassword()).isEqualTo("s3cret!");
		assertThat(user.getProfileId()).isEqualTo(SALESPERSON.id());
		assertThat(user.isTwoFactorEnabled()).isFalse();
	}

	@Test
	@DisplayName("Forces two-factor on when the profile is administrator")
	void shouldForceTwoFactorEnabledWhenProfileIsAdministrator() {
		User user = User.register("Admin User", "admin@example.com", "s3cret!", ADMINISTRATOR);

		assertThat(user.isTwoFactorEnabled()).isTrue();
	}

	@Test
	@DisplayName("Rejects a blank name")
	void shouldRejectBlankName() {
		assertThatThrownBy(() -> User.register(" ", "jane@example.com", "s3cret!", SALESPERSON))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects an invalid email")
	void shouldRejectInvalidEmail() {
		assertThatThrownBy(() -> User.register("Jane Doe", "not-an-email", "s3cret!", SALESPERSON))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects a blank password")
	void shouldRejectBlankPassword() {
		assertThatThrownBy(() -> User.register("Jane Doe", "jane@example.com", " ", SALESPERSON))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects a missing profile")
	void shouldRejectMissingProfile() {
		assertThatThrownBy(() -> User.register("Jane Doe", "jane@example.com", "s3cret!", null))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Registers the user as active")
	void shouldRegisterUserAsActive() {
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);

		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("Applies only non-null fields on update")
	void shouldApplyOnlyNonNullFieldsOnUpdate() {
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);

		user.update("Jane Roe", null, null, null);

		assertThat(user.getName()).isEqualTo("Jane Roe");
		assertThat(user.getEmail()).isEqualTo("jane@example.com");
		assertThat(user.getProfileId()).isEqualTo(SALESPERSON.id());
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("Forces two-factor on when the profile switches to administrator")
	void shouldForceTwoFactorEnabledWhenProfileSwitchesToAdministrator() {
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);

		user.update(null, null, ADMINISTRATOR, null);

		assertThat(user.getProfileId()).isEqualTo(ADMINISTRATOR.id());
		assertThat(user.isTwoFactorEnabled()).isTrue();
	}

	@Test
	@DisplayName("Deactivates the user on update")
	void shouldDeactivateUserOnUpdate() {
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);

		user.update(null, null, null, UserStatus.INACTIVE);

		assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
	}

	@Test
	@DisplayName("Rejects a blank name on update")
	void shouldRejectBlankNameOnUpdate() {
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);

		assertThatThrownBy(() -> user.update(" ", null, null, null))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects an invalid email on update")
	void shouldRejectInvalidEmailOnUpdate() {
		User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);

		assertThatThrownBy(() -> user.update(null, "not-an-email", null, null))
				.isInstanceOf(BusinessRuleException.class);
	}
}
