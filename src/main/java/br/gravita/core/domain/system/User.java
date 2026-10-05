package br.gravita.core.domain.system;

import br.gravita.core.domain.shared.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.regex.Pattern;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

	private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	private UserId id;
	private String name;
	private String email;
	private String rawPassword;
	private UUID profileId;
	private UUID companyId;
	private boolean twoFactorEnabled;
	private UserStatus status;

	public static User register(String name, String email, String rawPassword, ProfileReference profile) {
		return register(name, email, rawPassword, profile, null);
	}

	/** Registers a user that belongs to the tenant company {@code companyId}. */
	public static User register(String name, String email, String rawPassword, ProfileReference profile, UUID companyId) {
		return create(name, email, rawPassword, profile, companyId, UserStatus.ACTIVE);
	}

	/** Self-service signup: the account stays locked until the owner confirms the activation e-mail. */
	public static User signUp(String name, String email, String rawPassword, ProfileReference profile, UUID companyId) {
		return create(name, email, rawPassword, profile, companyId, UserStatus.PENDING_ACTIVATION);
	}

	private static User create(String name, String email, String rawPassword, ProfileReference profile, UUID companyId,
			UserStatus status) {
		validate(name, email, rawPassword, profile);
		return User.builder()
				.id(UserId.generate())
				.name(name)
				.email(email)
				.rawPassword(rawPassword)
				.profileId(profile.id())
				.companyId(companyId)
				.twoFactorEnabled(profile.isAdministrator())
				.status(status)
				.build();
	}

	public boolean isPendingActivation() {
		return status == UserStatus.PENDING_ACTIVATION;
	}

	/** Only a pending signup can be activated; an administrator-deactivated account must not be reopened this way. */
	public void activate() {
		if (!isPendingActivation()) {
			throw new BusinessRuleException("Only a user pending activation can be activated");
		}
		this.status = UserStatus.ACTIVE;
	}

	public boolean isActive() {
		return status == UserStatus.ACTIVE;
	}

	/** Same bar as registration: the new password only has to be present. */
	public void changePassword(String newRawPassword) {
		requirePassword(newRawPassword);
		this.rawPassword = newRawPassword;
	}

	public void update(String name, String email, ProfileReference profile, UserStatus status) {
		if (name != null) {
			if (name.isBlank()) {
				throw new BusinessRuleException("Name is required");
			}
			this.name = name;
		}
		if (email != null) {
			if (!EMAIL.matcher(email).matches()) {
				throw new BusinessRuleException("A valid email is required");
			}
			this.email = email;
		}
		if (profile != null) {
			this.profileId = profile.id();
			if (profile.isAdministrator()) {
				this.twoFactorEnabled = true;
			}
		}
		if (status != null) {
			this.status = status;
		}
	}

	private static void validate(String name, String email, String rawPassword, ProfileReference profile) {
		if (name == null || name.isBlank()) {
			throw new BusinessRuleException("Name is required");
		}
		if (email == null || !EMAIL.matcher(email).matches()) {
			throw new BusinessRuleException("A valid email is required");
		}
		requirePassword(rawPassword);
		if (profile == null) {
			throw new BusinessRuleException("Profile is required");
		}
	}

	private static void requirePassword(String rawPassword) {
		if (rawPassword == null || rawPassword.isBlank()) {
			throw new BusinessRuleException("Password is required");
		}
	}
}
