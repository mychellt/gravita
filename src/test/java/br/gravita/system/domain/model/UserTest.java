package br.gravita.system.domain.model;

import br.gravita.shared.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

	private static final ProfileReference SALESPERSON = new ProfileReference(UUID.randomUUID(), "Salesperson");
	private static final ProfileReference ADMINISTRATOR = new ProfileReference(UUID.randomUUID(), "Administrator");

	@Test
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
	void shouldForceTwoFactorEnabledWhenProfileIsAdministrator() {
		User user = User.register("Admin User", "admin@example.com", "s3cret!", ADMINISTRATOR);

		assertThat(user.isTwoFactorEnabled()).isTrue();
	}

	@Test
	void shouldRejectBlankName() {
		assertThatThrownBy(() -> User.register(" ", "jane@example.com", "s3cret!", SALESPERSON))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldRejectInvalidEmail() {
		assertThatThrownBy(() -> User.register("Jane Doe", "not-an-email", "s3cret!", SALESPERSON))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldRejectBlankPassword() {
		assertThatThrownBy(() -> User.register("Jane Doe", "jane@example.com", " ", SALESPERSON))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldRejectMissingProfile() {
		assertThatThrownBy(() -> User.register("Jane Doe", "jane@example.com", "s3cret!", null))
				.isInstanceOf(BusinessRuleException.class);
	}
}
