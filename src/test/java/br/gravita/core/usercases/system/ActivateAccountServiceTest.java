package br.gravita.core.usercases.system;

import br.gravita.core.domain.system.*;
import br.gravita.core.domain.system.ActivationRejectedException.Reason;
import br.gravita.core.ports.outbound.persistence.system.ActivationTokenRepositoryPort;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivateAccountServiceTest {

    private static final LocalDateTime ISSUED_AT = LocalDateTime.parse("2026-01-10T12:00:00");
    private static final ProfileReference ADMINISTRATOR = new ProfileReference(UUID.randomUUID(), "Administrator");

    @Mock
    private ActivationTokenRepositoryPort tokenRepository;
    @Mock
    private UserRepositoryPort userRepository;

    private User user;
    private IssuedActivationToken issued;

    @BeforeEach
    void setUp() {
        user = User.signUp("Ana Souza", "ana@acme.com", "s3cret-pass", ADMINISTRATOR, UUID.randomUUID());
        issued = ActivationToken.issue(user.getId(), ISSUED_AT);
    }

    private ActivateAccountService serviceAt(final LocalDateTime now) {
        return new ActivateAccountService(tokenRepository, userRepository,
                Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    private void givenStoredToken() {
        when(tokenRepository.findByTokenHashForUpdate(issued.token().getTokenHash()))
                .thenReturn(Optional.of(issued.token()));
    }

    private void givenStoredUser() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("A valid token activates the user and is marked used")
    void shouldActivateUserAndSpendTheToken() {
        givenStoredToken();
        givenStoredUser();


        serviceAt(ISSUED_AT.plus(Duration.ofHours(1))).execute(issued.rawToken());

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(issued.token().getModifiedAt()).isEqualTo(ISSUED_AT.plus(Duration.ofHours(1)));
        verify(userRepository).update(user);
        verify(tokenRepository).save(issued.token());
    }

    @Test
    @DisplayName("Replaying a spent token is rejected and leaves the (already active) user untouched")
    void shouldRejectAReplayedToken() {
        givenStoredToken();
        givenStoredUser();
        final ActivateAccountService service = serviceAt(ISSUED_AT.plusSeconds(60));
        service.execute(issued.rawToken());

        assertThatThrownBy(() -> service.execute(issued.rawToken()))
                .isInstanceOfSatisfying(ActivationRejectedException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.ALREADY_USED));

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).update(user); // once: the replay did not re-process it
    }

    @Test
    @DisplayName("An expired token is rejected and the user is not activated")
    void shouldRejectAnExpiredToken() {
        givenStoredToken();
        givenStoredUser();

        assertThatThrownBy(() -> serviceAt(ISSUED_AT.plus(Duration.ofHours(25))).execute(issued.rawToken()))
                .isInstanceOfSatisfying(ActivationRejectedException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.EXPIRED));

        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
        verify(userRepository, never()).update(any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("An unknown token is rejected as invalid")
    void shouldRejectAnUnknownToken() {
        when(tokenRepository.findByTokenHashForUpdate(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceAt(ISSUED_AT).execute("not-a-real-token"))
                .isInstanceOfSatisfying(ActivationRejectedException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.INVALID));
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("A missing or blank token is rejected as invalid without touching the database")
    void shouldRejectAMissingToken() {
        for (final String token : new String[]{null, "", "   "}) {
            assertThatThrownBy(() -> serviceAt(ISSUED_AT).execute(token))
                    .isInstanceOfSatisfying(ActivationRejectedException.class,
                            e -> assertThat(e.getReason()).isEqualTo(Reason.INVALID));
        }
        verifyNoInteractions(tokenRepository, userRepository);
    }

    @Test
    @DisplayName("A token whose user is no longer pending cannot reopen the account and is not spent")
    void shouldNotReactivateADeactivatedUser() {
        user.update(null, null, null, UserStatus.INACTIVE);
        givenStoredToken();
        givenStoredUser();

        assertThatThrownBy(() -> serviceAt(ISSUED_AT.plusSeconds(60)).execute(issued.rawToken()))
                .isInstanceOfSatisfying(ActivationRejectedException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.INVALID));

        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        verify(userRepository, never()).update(any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Business rule 6: an expired link of a deactivated user is invalid, not merely expired")
    void shouldTreatAnExpiredLinkOfADeactivatedUserAsInvalid() {
        user.update(null, null, null, UserStatus.INACTIVE);
        givenStoredToken();
        givenStoredUser();

        assertThatThrownBy(() -> serviceAt(ISSUED_AT.plus(Duration.ofHours(25))).execute(issued.rawToken()))
                .isInstanceOfSatisfying(ActivationRejectedException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.INVALID));
    }
}
