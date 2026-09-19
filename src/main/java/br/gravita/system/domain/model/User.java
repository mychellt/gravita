package br.gravita.system.domain.model;

import br.gravita.shared.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * User aggregate root (doc §11.1): credentials, an assigned profile and the 2FA flag. Password
 * hashing (doc §11.4, bcrypt) is a persistence-adapter concern, not a domain one - {@code
 * rawPassword} here is plaintext from the domain's point of view, mirroring how {@code
 * IntegrationCredential} treats its own secret payload.
 */
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

	public static User register(String name, String email, String rawPassword, ProfileReference profile) {
		validate(name, email, rawPassword, profile);
		return User.builder()
				.id(UserId.generate())
				.name(name)
				.email(email)
				.rawPassword(rawPassword)
				.profileId(profile.id())
				.twoFactorEnabled(profile.isAdministrator())
				.build();
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
