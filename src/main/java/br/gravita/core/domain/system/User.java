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
	private boolean twoFactorEnabled;
	private UserStatus status;

	public static User register(String name, String email, String rawPassword, ProfileReference profile) {
		validate(name, email, rawPassword, profile);
		return User.builder()
				.id(UserId.generate())
				.name(name)
				.email(email)
				.rawPassword(rawPassword)
				.profileId(profile.id())
				.twoFactorEnabled(profile.isAdministrator())
				.status(UserStatus.ACTIVE)
				.build();
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
		if (rawPassword == null || rawPassword.isBlank()) {
			throw new BusinessRuleException("Password is required");
		}
		if (profile == null) {
			throw new BusinessRuleException("Profile is required");
		}
	}
}
