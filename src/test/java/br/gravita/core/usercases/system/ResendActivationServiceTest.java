package br.gravita.core.usercases.system;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.system.*;
import br.gravita.core.ports.messaging.NotifyUserRegistrationProducerPort;
import br.gravita.core.ports.outbound.persistence.system.ActivationTokenRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResendActivationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-10T12:00:00Z");
    private static final ProfileReference ADMINISTRATOR = new ProfileReference(UUID.randomUUID(), "Administrator");

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private ActivationTokenRepositoryPort tokenRepository;
    @Mock
    private NotifyUserRegistrationProducerPort publisher;

    private User pendingUser;

    @BeforeEach
    void setUp() {
        pendingUser = User.signUp("Ana Souza", "ana@acme.com", "s3cret-pass", ADMINISTRATOR, UUID.randomUUID());
    }

    private ResendActivationService service() {
        return new ResendActivationService(userRepository, tokenRepository, publisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void givenLatestTokenIssuedAt(Instant issuedAt) {
        when(tokenRepository.findLatestByUserId(pendingUser.getId().value()))
                .thenReturn(Optional.of(ActivationToken.issue(pendingUser.getId(), issuedAt).token()));
    }

    @Test
    @DisplayName("AC7: a pending user whose last token is older than the cooldown gets a new activation e-mail requested")
    void shouldRequestAnEmailForAPendingUser() {
        when(userRepository.findByEmail("ana@acme.com")).thenReturn(Optional.of(pendingUser));
        givenLatestTokenIssuedAt(NOW.minusSeconds(61));

        service().execute("  ana@acme.com ");

        verify(publisher).execute(eventContext());
    }

    @Test
    @DisplayName("AC7: a pending user with no token at all (the first mail never went out) can resend")
    void shouldRequestAnEmailWhenNoTokenWasEverIssued() {
        when(userRepository.findByEmail("ana@acme.com")).thenReturn(Optional.of(pendingUser));
        when(tokenRepository.findLatestByUserId(pendingUser.getId().value())).thenReturn(Optional.empty());

        service().execute("ana@acme.com");

        verify(publisher).execute(eventContext());
    }

    @Test
    @DisplayName("AC8: an unknown e-mail is a silent no-op")
    void shouldDoNothingForAnUnknownEmail() {
        when(userRepository.findByEmail("ghost@acme.com")).thenReturn(Optional.empty());

        service().execute("ghost@acme.com");

        verifyNoInteractions(publisher, tokenRepository);
    }

    @Test
    @DisplayName("AC8: an already active user is a silent no-op")
    void shouldDoNothingForAnActiveUser() {
        pendingUser.activate();
        when(userRepository.findByEmail("ana@acme.com")).thenReturn(Optional.of(pendingUser));

        service().execute("ana@acme.com");

        verifyNoInteractions(publisher, tokenRepository);
    }

    @Test
    @DisplayName("A deactivated user is not reopened through a resend")
    void shouldDoNothingForAnInactiveUser() {
        pendingUser.update(null, null, null, UserStatus.INACTIVE);
        when(userRepository.findByEmail("ana@acme.com")).thenReturn(Optional.of(pendingUser));

        service().execute("ana@acme.com");

        verifyNoInteractions(publisher, tokenRepository);
    }

    @Test
    @DisplayName("AC9: a second call inside the 60 second cooldown requests nothing")
    void shouldDoNothingInsideTheCooldown() {
        when(userRepository.findByEmail("ana@acme.com")).thenReturn(Optional.of(pendingUser));
        givenLatestTokenIssuedAt(NOW.minusSeconds(59));

        service().execute("ana@acme.com");

        verifyNoInteractions(publisher);
    }

    @Test
    @DisplayName("A blank or missing e-mail is a no-op")
    void shouldDoNothingForABlankEmail() {
        service().execute(" ");
        service().execute(null);

        verifyNoInteractions(userRepository, tokenRepository, publisher);
    }

    private Context eventContext() {
        return argThat(context -> new ActivationEmailRequested(pendingUser.getId(), "Ana Souza", "ana@acme.com")
                .equals(context.getData(ActivationEmailRequested.class)));
    }
}
