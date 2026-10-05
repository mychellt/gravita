package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.system.ActivationToken;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserStatus;
import br.gravita.core.ports.messaging.NotifyUserRegistrationProducerPort;
import br.gravita.core.ports.messaging.records.NotifyUserRegistrationMessage;
import br.gravita.core.ports.outbound.persistence.system.ActivationTokenRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@UseCase
public class ResendActivationService implements ResendActivationUseCase {

    static final Duration COOLDOWN = Duration.ofSeconds(60);

    private final UserRepositoryPort userRepositoryPort;
    private final ActivationTokenRepositoryPort tokenRepositoryPort;
    private final NotifyUserRegistrationProducerPort notifyUserRegistrationProducerPort;
    private final Clock clock = Clock.systemDefaultZone();

    @Override
    @Transactional
    public void execute(final String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        userRepositoryPort.findByEmail(email.strip())
                .filter(user -> user.getStatus() == UserStatus.PENDING_ACTIVATION)
                .filter(this::isOutsideCooldown)
                .ifPresent(this::issueNewLink);
    }

    private void issueNewLink(final User user) {
        final var now = LocalDateTime.now(clock);
        tokenRepositoryPort.findUnusedByUserId(user.getId().value()).forEach(previous -> {
            previous.expire(now);
            tokenRepositoryPort.save(previous);
        });

        final var issued = ActivationToken.issue(user.getId(), now);
        tokenRepositoryPort.save(issued.token());
        notifyUserRegistrationProducerPort.execute(new Context(NotifyUserRegistrationMessage.builder()
                .username(user.getName())
                .recipient(user.getEmail())
                .token(issued.rawToken())
                .tenantId(user.getCompanyId())
                .build()));
    }

    private boolean isOutsideCooldown(final User user) {
        return tokenRepositoryPort.findLatestByUserId(user.getId().value())
                .map(latest -> !latest.wasIssuedWithin(COOLDOWN, LocalDateTime.now(clock)))
                .orElse(true);
    }
}
