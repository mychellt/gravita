package br.gravita.core.usercases.system;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.*;
import br.gravita.core.domain.system.PasswordResetRejectedException.Reason;
import br.gravita.core.ports.outbound.persistence.system.PasswordResetTokenRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmPasswordResetServiceTest {

    private static final LocalDateTime ISSUED_AT = LocalDateTime.parse("2026-01-10T12:00:00");
    private static final ProfileReference ADMINISTRATOR = new ProfileReference(UUID.randomUUID(), "Administrator");

    @Mock
    private PasswordResetTokenRepositoryPort tokenRepository;
    @Mock
    private UserRepositoryPort userRepository;

    private User user;
    private IssuedPasswordResetToken issued;

    @BeforeEach
    void setUp() {
        user = User.register("Ana Souza", "ana@acme.com", "old-pass", ADMINISTRATOR, UUID.randomUUID());
        issued = PasswordResetToken.issue(user.getId(), ISSUED_AT);
    }

    private ConfirmPasswordResetService serviceAt(final LocalDateTime now) {
        return new ConfirmPasswordResetService(tokenRepository, userRepository,
                Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    private void givenStoredToken() {
        when(tokenRepository.findByTokenHashForUpdate(issued.token().getTokenHash()))
                .thenReturn(Optional.of(issued.token()));
    }

    private void givenStoredUser() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    private void assertRejectedWith(final Reason reason, final Runnable call) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(PasswordResetRejectedException.class,
                e -> assertThat(e.getReason()).isEqualTo(reason));
    }

    @Test
    @DisplayName("AC10: a valid token changes the password, spends the token and stores the new password")
    void shouldChangeThePasswordAndSpendTheToken() {
        givenStoredToken();
        givenStoredUser();
        when(tokenRepository.findUnusedByUserId(user.getId().value())).thenReturn(List.of());

        serviceAt(ISSUED_AT.plusMinutes(30)).execute(issued.rawToken(), "n3w-pass");

        assertThat(user.getRawPassword()).isEqualTo("n3w-pass");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(issued.token().getModifiedAt()).isEqualTo(ISSUED_AT.plusMinutes(30));
        verify(userRepository).updatePassword(user);
        verify(tokenRepository).save(issued.token());
    }

    @Test
    @DisplayName("AC10: every other unused token of the user is invalidated, the spent one is left alone")
    void shouldInvalidateEveryOtherUnusedToken() {
        final var now = ISSUED_AT.plusMinutes(30);
        final var other = PasswordResetToken.issue(user.getId(), ISSUED_AT.plusMinutes(10)).token();
        givenStoredToken();
        givenStoredUser();
        when(tokenRepository.findUnusedByUserId(user.getId().value())).thenReturn(List.of(other));

        serviceAt(now).execute(issued.rawToken(), "n3w-pass");

        assertThat(other.getExpiresAt()).isEqualTo(now);
        verify(tokenRepository).save(other);
        assertThat(issued.token().getExpiresAt()).isEqualTo(ISSUED_AT.plusHours(1));
    }

    @Test
    @DisplayName("AC10: the use case establishes no session: it returns nothing and only the password is written")
    void shouldOnlyWriteThePassword() {
        givenStoredToken();
        givenStoredUser();
        when(tokenRepository.findUnusedByUserId(user.getId().value())).thenReturn(List.of());

        serviceAt(ISSUED_AT.plusMinutes(1)).execute(issued.rawToken(), "n3w-pass");

        verify(userRepository, never()).update(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC11: replaying a spent token is rejected as already used and does not change the password again")
    void shouldRejectAReplayedToken() {
        givenStoredToken();
        givenStoredUser();
        when(tokenRepository.findUnusedByUserId(user.getId().value())).thenReturn(List.of());
        final ConfirmPasswordResetService service = serviceAt(ISSUED_AT.plusMinutes(1));
        service.execute(issued.rawToken(), "n3w-pass");

        assertRejectedWith(Reason.ALREADY_USED, () -> service.execute(issued.rawToken(), "other-pass"));

        assertThat(user.getRawPassword()).isEqualTo("n3w-pass");
        verify(userRepository, times(1)).updatePassword(user);
    }

    @Test
    @DisplayName("AC11: an expired token is rejected, the password stays and nothing is saved")
    void shouldRejectAnExpiredToken() {
        givenStoredToken();
        givenStoredUser();

        assertRejectedWith(Reason.EXPIRED, () -> serviceAt(ISSUED_AT.plus(Duration.ofHours(1)))
                .execute(issued.rawToken(), "n3w-pass"));

        assertThat(user.getRawPassword()).isEqualTo("old-pass");
        verify(userRepository, never()).updatePassword(any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC11: a token expired by a newer request is rejected as expired")
    void shouldRejectATokenCutShortByANewerRequest() {
        issued.token().expire(ISSUED_AT.plusMinutes(5));
        givenStoredToken();
        givenStoredUser();

        assertRejectedWith(Reason.EXPIRED, () -> serviceAt(ISSUED_AT.plusMinutes(6))
                .execute(issued.rawToken(), "n3w-pass"));

        verify(userRepository, never()).updatePassword(any());
    }

    @Test
    @DisplayName("AC11: an unknown token is rejected as invalid")
    void shouldRejectAnUnknownToken() {
        when(tokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.empty());

        assertRejectedWith(Reason.INVALID, () -> serviceAt(ISSUED_AT).execute("not-a-real-token", "n3w-pass"));

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("AC11: a missing or blank token is rejected as invalid without touching the database")
    void shouldRejectAMissingToken() {
        for (final String token : new String[]{null, "", "   "}) {
            assertRejectedWith(Reason.INVALID, () -> serviceAt(ISSUED_AT).execute(token, "n3w-pass"));
        }
        verifyNoInteractions(tokenRepository, userRepository);
    }

    @Test
    @DisplayName("An activation token is not a reset token: its secret finds nothing in the reset tokens")
    void shouldNotAcceptAnActivationSecret() {
        final var activation = ActivationToken.issue(user.getId(), ISSUED_AT);
        // The reset repository only ever holds reset tokens, so an activation secret is simply unknown to it.
        when(tokenRepository.findByTokenHashForUpdate(PasswordResetToken.hash(activation.rawToken())))
                .thenReturn(Optional.empty());

        assertRejectedWith(Reason.INVALID, () -> serviceAt(ISSUED_AT).execute(activation.rawToken(), "n3w-pass"));
    }

    @Test
    @DisplayName("A token of a deactivated or still pending user cannot change the password and is not spent")
    void shouldNotResetThePasswordOfAnInactiveUser() {
        user.update(null, null, null, UserStatus.INACTIVE);
        givenStoredToken();
        givenStoredUser();

        assertRejectedWith(Reason.INVALID, () -> serviceAt(ISSUED_AT.plusMinutes(1))
                .execute(issued.rawToken(), "n3w-pass"));

        assertThat(user.getRawPassword()).isEqualTo("old-pass");
        verify(userRepository, never()).updatePassword(any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("An expired link of a deactivated user is invalid, not merely expired")
    void shouldTreatAnExpiredLinkOfADeactivatedUserAsInvalid() {
        user.update(null, null, null, UserStatus.INACTIVE);
        givenStoredToken();
        givenStoredUser();

        assertRejectedWith(Reason.INVALID, () -> serviceAt(ISSUED_AT.plusHours(2))
                .execute(issued.rawToken(), "n3w-pass"));
    }

    @Test
    @DisplayName("A blank new password is refused and the token is not spent")
    void shouldRefuseABlankPassword() {
        givenStoredToken();
        givenStoredUser();

        assertThatThrownBy(() -> serviceAt(ISSUED_AT.plusMinutes(1)).execute(issued.rawToken(), "  "))
                .isInstanceOf(BusinessRuleException.class)
                .isNotInstanceOf(PasswordResetRejectedException.class);

        assertThat(user.getRawPassword()).isEqualTo("old-pass");
        verify(userRepository, never()).updatePassword(any());
        verify(tokenRepository, never()).save(any());
    }
}
