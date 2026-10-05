package br.gravita.core.usercases.system;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.system.ActivationToken;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserStatus;
import br.gravita.core.ports.messaging.NotifyUserRegistrationProducerPort;
import br.gravita.core.ports.messaging.records.NotifyUserRegistrationMessage;
import br.gravita.core.ports.outbound.persistence.system.ActivationTokenRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResendActivationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.parse("2026-01-10T12:00:00");
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
        return new ResendActivationService(userRepository, tokenRepository, publisher);
    }

    private void givenLatestTokenIssuedAt(final LocalDateTime issuedAt) {
        when(tokenRepository.findLatestByUserId(pendingUser.getId().value()))
                .thenReturn(Optional.of(ActivationToken.issue(pendingUser.getId(), issuedAt).token()));
    }

    @Test
    @DisplayName("AC7: a pending user whose last token is older than the cooldown gets a new activation e-mail requested")
    void shouldRequestAnEmailForAPendingUser() {
        when(userRepository.findByEmail("ana@acme.com")).thenReturn(Optional.of(pendingUser));
        givenLatestTokenIssuedAt(NOW.minusSeconds(61));

        service().execute("  ana@acme.com ");

        verify(publisher).execute(registrationContext());
    }

    @Test
    @DisplayName("AC7: the new e-mail carries a freshly issued token, saved so that only the new link works")
    void shouldIssueAndPublishANewToken() {
        when(userRepository.findByEmail("ana@acme.com")).thenReturn(Optional.of(pendingUser));
        givenLatestTokenIssuedAt(NOW.minusSeconds(61));
        when(tokenRepository.findUnusedByUserId(pendingUser.getId().value())).thenReturn(List.of());

        service().execute("ana@acme.com");

        final var saved = ArgumentCaptor.forClass(ActivationToken.class);
        verify(tokenRepository).save(saved.capture());
        final var message = publishedMessage();
        assertThat(ActivationToken.hash(message.token())).isEqualTo(saved.getValue().getTokenHash());
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(ActivationToken.VALIDITY));
        assertThat(message.tenantId()).isEqualTo(pendingUser.getCompanyId());
    }

    @Test
    @DisplayName("Business rule 5: the previous unused links stop working when a new one is requested")
    void shouldExpirePreviousUnusedTokens() {
        when(userRepository.findByEmail("ana@acme.com")).thenReturn(Optional.of(pendingUser));
        final var previous = ActivationToken.issue(pendingUser.getId(), NOW.minusHours(2)).token();
        when(tokenRepository.findLatestByUserId(pendingUser.getId().value())).thenReturn(Optional.of(previous));
        when(tokenRepository.findUnusedByUserId(pendingUser.getId().value())).thenReturn(List.of(previous));

        service().execute("ana@acme.com");

        assertThat(previous.getExpiresAt()).isEqualTo(NOW);
        verify(tokenRepository).save(previous);
    }

    @Test
    @DisplayName("AC7: a pending user with no token at all (the first mail never went out) can resend")
    void shouldRequestAnEmailWhenNoTokenWasEverIssued() {
        when(userRepository.findByEmail("ana@acme.com")).thenReturn(Optional.of(pendingUser));
        when(tokenRepository.findLatestByUserId(pendingUser.getId().value())).thenReturn(Optional.empty());

        service().execute("ana@acme.com");

        verify(publisher).execute(registrationContext());
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

    private Context registrationContext() {
        return argThat(context -> {
            final var message = context.getData(NotifyUserRegistrationMessage.class);
            return message.username().equals("Ana Souza") && message.recipient().equals("ana@acme.com");
        });
    }

    private NotifyUserRegistrationMessage publishedMessage() {
        final var context = ArgumentCaptor.forClass(Context.class);
        verify(publisher).execute(context.capture());
        return context.getValue().getData(NotifyUserRegistrationMessage.class);
    }
}
