package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.system.PasswordResetRejectedException;
import br.gravita.core.domain.system.PasswordResetRejectedException.Reason;
import br.gravita.core.domain.system.PasswordResetToken;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.persistence.system.PasswordResetTokenRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@UseCase
public class ConfirmPasswordResetService implements ConfirmPasswordResetUseCase {

    private final PasswordResetTokenRepositoryPort tokenRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final Clock clock;

    @Autowired
    public ConfirmPasswordResetService(PasswordResetTokenRepositoryPort tokenRepositoryPort,
                                       UserRepositoryPort userRepositoryPort) {
        this(tokenRepositoryPort, userRepositoryPort, Clock.systemDefaultZone());
    }

    ConfirmPasswordResetService(PasswordResetTokenRepositoryPort tokenRepositoryPort,
                                UserRepositoryPort userRepositoryPort, Clock clock) {
        this.tokenRepositoryPort = tokenRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void execute(final String rawToken, final String newPassword) {
        final var token = findToken(rawToken);
        final var user = userRepositoryPort.findById(UserId.of(token.getUserId()))
                .orElseThrow(ConfirmPasswordResetService::invalidToken);
        // A spent link must still say "already used"; any other link of a user who is not active (blocked, or still
        // pending activation) is plainly invalid, even when it has also expired.
        if (!token.isUsed() && !user.isActive()) {
            throw invalidToken();
        }

        final var now = LocalDateTime.now(clock);
        token.consume(now);
        user.changePassword(newPassword);

        userRepositoryPort.updatePassword(user);
        tokenRepositoryPort.save(token);
        invalidateOtherLinks(user.getId(), now);
    }

    /** The reset is done: no other e-mailed link of this user may change the password again. */
    private void invalidateOtherLinks(UserId userId, LocalDateTime now) {
        tokenRepositoryPort.findUnusedByUserId(userId.value()).forEach(other -> {
            other.expire(now);
            tokenRepositoryPort.save(other);
        });
    }

    private PasswordResetToken findToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidToken();
        }
        return tokenRepositoryPort.findByTokenHashForUpdate(PasswordResetToken.hash(rawToken.strip()))
                .orElseThrow(ConfirmPasswordResetService::invalidToken);
    }

    private static PasswordResetRejectedException invalidToken() {
        return new PasswordResetRejectedException(Reason.INVALID, "Link de redefinição inválido.");
    }
}
