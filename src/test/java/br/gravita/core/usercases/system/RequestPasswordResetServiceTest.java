package br.gravita.core.usercases.system;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.system.PasswordResetToken;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserStatus;
import br.gravita.core.ports.messaging.NotifyPasswordResetProducerPort;
import br.gravita.core.ports.messaging.records.NotifyPasswordResetMessage;
import br.gravita.core.ports.outbound.persistence.system.PasswordResetTokenRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestPasswordResetServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.parse("2026-01-10T12:00:00");
    private static final ProfileReference ADMINISTRATOR = new ProfileReference(UUID.randomUUID(), "Administrator");

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private PasswordResetTokenRepositoryPort tokenRepository;
    @Mock
    private NotifyPasswordResetProducerPort publisher;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.register("Ana Souza", "ana@acme.com", "s3cret-pass", ADMINISTRATOR, UUID.randomUUID());
    }

    private RequestPasswordResetService service() {
        return new RequestPasswordResetService(userRepository, tokenRepository, publisher,
                Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    private void givenLatestTokenIssuedAt(final LocalDateTime issuedAt) {
        when(tokenRepository.findLatestByUserId(activeUser.getId().value()))
                .thenReturn(Optional.of(PasswordResetToken.issue(activeUser.getId(), issuedAt).token()));
    }

    @Test
    @DisplayName("AC9: an active user whose last token is older than the cooldown gets exactly one reset e-mail requested")
    void shouldRequestOneEmailForAnActiveUser() {
        when(userRepository.findByEmail("ana@acme.com")).thenReturn(Optional.of(activeUser));
        givenLatestTokenIssuedAt(NOW.minusSeconds(61));

        service().execute("  ana@acme.com ");

        verify(publisher, times(1)).execute(resetContext());
    }

    @Test
    @DisplayName("AC9: the e-mail carries a freshly issued token that is valid for one hour and saved by hash only")
    void shouldIssueAndPublishAOneHourToken() {
        when(userRepository.findByEmail("ana@acme.com")).thenReturn(Optional.of(activeUser));
        givenLatestTokenIssuedAt(NOW.minusSeconds(61));
        when(tokenRepository.findUnusedByUserId(activeUser.getId().value())).thenReturn(List.of());

        service().execute("ana@acme.com");

        final var saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(saved.capture());
        final var message = publishedMessage();
        assertThat(PasswordResetToken.hash(message.token())).isEqualTo(saved.getValue().getTokenHash());
        assertThat(saved.getValue().getTokenHash()).isNotEqualTo(message.token());
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plusHours(1));
        assertThat(saved.getValue().getUserId()).isEqualTo(activeUser.getId().value());
        assertThat(message.tenantId()).isEqualTo(activeUser.getCompanyId());
    }

    @Test
    @DisplayName("Business rule 6: the previous unused links stop working when a new one is requested")
    void shouldExpirePreviousUnusedTokens() {
        when(userRepository.findByEmail("ana@acme.com")).thenReturn(Optional.of(activeUser));
        final var previous = PasswordResetToken.issue(activeUser.getId(), NOW.minusMinutes(5)).token();
        when(tokenRepository.findLatestByUserId(activeUser.getId().value())).thenReturn(Optional.of(previous));
        when(tokenRepository.findUnusedByUserId(activeUser.getId().value())).thenReturn(List.of(previous));

        service().execute("ana@acme.com");

        assertThat(previous.getExpiresAt()).isEqualTo(NOW);
        verify(tokenRepository).save(previous);
    }

    @Test
    @DisplayName("An active user who never asked before (no token at all) gets the e-mail")
    void shouldRequestAnEmailWhenNoTokenWasEverIssued() {
        when(userRepository.findByEmail("ana@acme.com")).thenReturn(Optional.of(activeUser));
        when(tokenRepository.findLatestByUserId(activeUser.getId().value())).thenReturn(Optional.empty());

        service().execute("ana@acme.com");

        verify(publisher).execute(resetContext());
    }

    @Test
    @DisplayName("AC8: an unknown e-mail is a silent no-op")
    void shouldDoNothingForAnUnknownEmail() {
        when(userRepository.findByEmail("ghost@acme.com")).thenReturn(Optional.empty());

        service().execute("ghost@acme.com");

        verifyNoInteractions(publisher, tokenRepository);
    }

    @Test
    @DisplayName("AC8: a user still pending activation is a silent no-op")
    void shouldDoNothingForAPendingUser() {
        final var pending = User.signUp("Bia Lima", "bia@acme.com", "s3cret-pass", ADMINISTRATOR, UUID.randomUUID());
        when(userRepository.findByEmail("bia@acme.com")).thenReturn(Optional.of(pending));

        service().execute("bia@acme.com");

        verifyNoInteractions(publisher, tokenRepository);
    }

    @Test
    @DisplayName("AC8: a deactivated user is a silent no-op")
    void shouldDoNothingForAnInactiveUser() {
        activeUser.update(null, null, null, UserStatus.INACTIVE);
        when(userRepository.findByEmail("ana@acme.com")).thenReturn(Optional.of(activeUser));

        service().execute("ana@acme.com");

        verifyNoInteractions(publisher, tokenRepository);
    }

    @Test
    @DisplayName("AC9: a second call inside the 60 second cooldown requests nothing and changes no token")
    void shouldDoNothingInsideTheCooldown() {
        when(userRepository.findByEmail("ana@acme.com")).thenReturn(Optional.of(activeUser));
        givenLatestTokenIssuedAt(NOW.minusSeconds(59));

        service().execute("ana@acme.com");

        verifyNoInteractions(publisher);
        verify(tokenRepository, never()).save(any());
        verify(tokenRepository, never()).findUnusedByUserId(any());
    }

    @Test
    @DisplayName("A blank or missing e-mail is a no-op")
    void shouldDoNothingForABlankEmail() {
        service().execute(" ");
        service().execute(null);

        verifyNoInteractions(userRepository, tokenRepository, publisher);
    }

    private Context resetContext() {
        return argThat(context -> {
            final var message = context.getData(NotifyPasswordResetMessage.class);
            return message.username().equals("Ana Souza") && message.recipient().equals("ana@acme.com");
        });
    }

    private NotifyPasswordResetMessage publishedMessage() {
        final var context = ArgumentCaptor.forClass(Context.class);
        verify(publisher).execute(context.capture());
        return context.getValue().getData(NotifyPasswordResetMessage.class);
    }
}
