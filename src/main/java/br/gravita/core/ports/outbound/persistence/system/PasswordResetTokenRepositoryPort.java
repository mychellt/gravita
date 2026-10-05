package br.gravita.core.ports.outbound.persistence.system;

import br.gravita.core.domain.system.PasswordResetToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepositoryPort {

	PasswordResetToken save(PasswordResetToken token);

	/** Locks the row so two concurrent submissions of the same link cannot both consume it. */
	Optional<PasswordResetToken> findByTokenHashForUpdate(String tokenHash);

	/** The user's most recently issued token, spent or not. */
	Optional<PasswordResetToken> findLatestByUserId(UUID userId);

	/** Tokens of the user that have not been consumed yet (they may already be past their expiry). */
	List<PasswordResetToken> findUnusedByUserId(UUID userId);
}
