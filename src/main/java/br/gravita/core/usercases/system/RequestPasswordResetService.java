package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.system.PasswordResetToken;
import br.gravita.core.domain.system.User;
import br.gravita.core.ports.messaging.NotifyPasswordResetProducerPort;
import br.gravita.core.ports.messaging.records.NotifyPasswordResetMessage;
import br.gravita.core.ports.outbound.persistence.system.PasswordResetTokenRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@UseCase
public class RequestPasswordResetService implements RequestPasswordResetUseCase {

    static final Duration COOLDOWN = Duration.ofSeconds(60);

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordResetTokenRepositoryPort tokenRepositoryPort;
    private final NotifyPasswordResetProducerPort notifyPasswordResetProducerPort;
    private final Clock clock;

    @Autowired
    public RequestPasswordResetService(UserRepositoryPort userRepositoryPort,
                                       PasswordResetTokenRepositoryPort tokenRepositoryPort,
                                       NotifyPasswordResetProducerPort notifyPasswordResetProducerPort) {
        this(userRepositoryPort, tokenRepositoryPort, notifyPasswordResetProducerPort, Clock.systemDefaultZone());
    }

    RequestPasswordResetService(UserRepositoryPort userRepositoryPort,
                                PasswordResetTokenRepositoryPort tokenRepositoryPort,
                                NotifyPasswordResetProducerPort notifyPasswordResetProducerPort, Clock clock) {
        this.userRepositoryPort = userRepositoryPort;
        this.tokenRepositoryPort = tokenRepositoryPort;
        this.notifyPasswordResetProducerPort = notifyPasswordResetProducerPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void execute(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        userRepositoryPort.findByEmail(email.strip())
                .filter(User::isActive)
                .filter(this::isOutsideCooldown)
                .ifPresent(this::issueNewLink);
    }

    /** Only the newest link may work, so the user's previous unused links are cut short before a new one is issued. */
    private void issueNewLink(User user) {
        final var now = LocalDateTime.now(clock);
        tokenRepositoryPort.findUnusedByUserId(user.getId().value()).forEach(previous -> {
            previous.expire(now);
            tokenRepositoryPort.save(previous);
        });

        final var issued = PasswordResetToken.issue(user.getId(), now);
        tokenRepositoryPort.save(issued.token());
        notifyPasswordResetProducerPort.execute(new Context(NotifyPasswordResetMessage.builder()
                .username(user.getName())
                .recipient(user.getEmail())
                .token(issued.rawToken())
                .tenantId(user.getCompanyId())
                .build()));
    }

    private boolean isOutsideCooldown(User user) {
        return tokenRepositoryPort.findLatestByUserId(user.getId().value())
                .map(latest -> !latest.wasIssuedWithin(COOLDOWN, LocalDateTime.now(clock)))
                .orElse(true);
    }
}
