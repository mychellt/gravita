package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class EmailVerification extends AbstractDomain {

	private static final int EXPIRATION_HOURS = 24;

	private IndividualPerson person;
	private String code;
	private LocalDateTime expiresAt;
	private LocalDateTime verifiedAt;

	public static EmailVerification requestFor(IndividualPerson person) {
		return EmailVerification.builder()
				.person(person)
				.code(generateCode())
				.expiresAt(LocalDateTime.now().plusHours(EXPIRATION_HOURS))
				.build();
	}

	public void confirm(String code) {
		if (verifiedAt != null) {
			throw new BusinessRuleException("Email already verified");
		}
		if (LocalDateTime.now().isAfter(expiresAt)) {
			throw new BusinessRuleException("Verification code expired");
		}
		if (!this.code.equals(code)) {
			throw new BusinessRuleException("Invalid verification code");
		}
		this.verifiedAt = LocalDateTime.now();
		person.setEmailVerifiedAt(this.verifiedAt);
	}

	private static String generateCode() {
		return String.valueOf(ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
	}
}
