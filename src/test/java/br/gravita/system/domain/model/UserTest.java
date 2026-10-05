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
		final User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);

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
		final User user = User.register("Admin User", "admin@example.com", "s3cret!", ADMINISTRATOR);

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
		final User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);

		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("Applies only non-null fields on update")
	void shouldApplyOnlyNonNullFieldsOnUpdate() {
		final User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);

		user.update("Jane Roe", null, null, null);

		assertThat(user.getName()).isEqualTo("Jane Roe");
		assertThat(user.getEmail()).isEqualTo("jane@example.com");
		assertThat(user.getProfileId()).isEqualTo(SALESPERSON.id());
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("Forces two-factor on when the profile switches to administrator")
	void shouldForceTwoFactorEnabledWhenProfileSwitchesToAdministrator() {
		final User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);

		user.update(null, null, ADMINISTRATOR, null);

		assertThat(user.getProfileId()).isEqualTo(ADMINISTRATOR.id());
		assertThat(user.isTwoFactorEnabled()).isTrue();
	}

	@Test
	@DisplayName("Deactivates the user on update")
	void shouldDeactivateUserOnUpdate() {
		final User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);

		user.update(null, null, null, UserStatus.INACTIVE);

		assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
	}

	@Test
	@DisplayName("Rejects a blank name on update")
	void shouldRejectBlankNameOnUpdate() {
		final User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);

		assertThatThrownBy(() -> user.update(" ", null, null, null))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects an invalid email on update")
	void shouldRejectInvalidEmailOnUpdate() {
		final User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);

		assertThatThrownBy(() -> user.update(null, "not-an-email", null, null))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("A self-service signup starts pending activation, not active")
	void shouldSignUpPendingActivation() {
		final User user = User.signUp("Ana Souza", "ana@acme.com", "s3cret!", ADMINISTRATOR, UUID.randomUUID());

		assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
		assertThat(user.isTwoFactorEnabled()).isTrue();
	}

	@Test
	@DisplayName("An administrator-registered user is still active right away")
	void shouldRegisterAsActive() {
		assertThat(User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON).getStatus())
				.isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("Activating a pending user makes it active")
	void shouldActivatePendingUser() {
		final User user = User.signUp("Ana Souza", "ana@acme.com", "s3cret!", ADMINISTRATOR, UUID.randomUUID());

		user.activate();

		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("Activation never reopens an administrator-deactivated or already active user")
	void shouldRejectActivatingAUserThatIsNotPending() {
		final User inactive = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);
		inactive.update(null, null, null, UserStatus.INACTIVE);
		final User active = User.register("John Doe", "john@example.com", "s3cret!", SALESPERSON);

		assertThatThrownBy(inactive::activate).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(active::activate).isInstanceOf(BusinessRuleException.class);
		assertThat(inactive.getStatus()).isEqualTo(UserStatus.INACTIVE);
	}

	@Test
	@DisplayName("Changing the password keeps the account as it is and only needs a non-blank password")
	void shouldChangeThePassword() {
		final User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);

		user.changePassword("n3w-pass");

		assertThat(user.getRawPassword()).isEqualTo("n3w-pass");
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("A blank or missing new password is refused and the old one is kept")
	void shouldRejectABlankNewPassword() {
		final User user = User.register("Jane Doe", "jane@example.com", "s3cret!", SALESPERSON);

		assertThatThrownBy(() -> user.changePassword(" ")).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> user.changePassword(null)).isInstanceOf(BusinessRuleException.class);
		assertThat(user.getRawPassword()).isEqualTo("s3cret!");
	}
}
