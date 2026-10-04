package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.system.ActivationEmailRequested;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserStatus;
import br.gravita.core.ports.messaging.NotifyUserRegistrationProducerPort;
import br.gravita.core.ports.outbound.persistence.system.ActivationTokenRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@UseCase
public class ResendActivationService implements ResendActivationUseCase {

    static final Duration COOLDOWN = Duration.ofSeconds(60);

    private final UserRepositoryPort userRepositoryPort;
    private final ActivationTokenRepositoryPort tokenRepositoryPort;
    private final NotifyUserRegistrationProducerPort notifyUserRegistrationProducerPort;
    private final Clock clock;

    @Autowired
    public ResendActivationService(UserRepositoryPort userRepositoryPort,
                                   ActivationTokenRepositoryPort tokenRepositoryPort,
                                   NotifyUserRegistrationProducerPort notifyUserRegistrationProducerPort) {
        this(userRepositoryPort, tokenRepositoryPort, notifyUserRegistrationProducerPort, Clock.systemDefaultZone());
    }

    ResendActivationService(UserRepositoryPort userRepositoryPort, ActivationTokenRepositoryPort tokenRepositoryPort,
                            NotifyUserRegistrationProducerPort notifyUserRegistrationProducerPort, Clock clock) {
        this.userRepositoryPort = userRepositoryPort;
        this.tokenRepositoryPort = tokenRepositoryPort;
        this.notifyUserRegistrationProducerPort = notifyUserRegistrationProducerPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void execute(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        userRepositoryPort.findByEmail(email.strip())
                .filter(user -> user.getStatus() == UserStatus.PENDING_ACTIVATION)
                .filter(this::isOutsideCooldown)
                .ifPresent(user -> notifyUserRegistrationProducerPort.execute(
                        new Context(new ActivationEmailRequested(user.getId(), user.getName(), user.getEmail()))));
    }

    private boolean isOutsideCooldown(User user) {
        return tokenRepositoryPort.findLatestByUserId(user.getId().value())
                .map(latest -> !latest.wasIssuedWithin(COOLDOWN, LocalDateTime.now(clock)))
                .orElse(true);
    }
}
