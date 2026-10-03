package br.gravita.core.usercases.system;

import br.gravita.core.domain.system.ActivationToken;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.messaging.ActivationEmailRequest;
import br.gravita.core.ports.messaging.SendActivationEmailPort;
import br.gravita.core.ports.outbound.persistence.system.ActivationTokenRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendActivationEmailServiceTest {

	private static final Instant NOW = Instant.parse("2026-01-10T12:00:00Z");

	@Mock
	private ActivationTokenRepositoryPort tokenRepository;
	@Mock
	private SendActivationEmailPort sendActivationEmail;

	private final UserId userId = UserId.generate();

	private SendActivationEmailService service() {
		return new SendActivationEmailService(tokenRepository, sendActivationEmail, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private SendActivationEmailCommand command() {
		return new SendActivationEmailCommand(userId, "Ana Souza", "ana@acme.com");
	}

	@Test
	@DisplayName("Issues a 24h token for the user, stores only its hash, and mails the raw secret")
	void shouldIssueAndMailAToken() {
		when(tokenRepository.save(any(ActivationToken.class))).thenAnswer(call -> call.getArgument(0));

		service().execute(command());

		ArgumentCaptor<ActivationToken> stored = ArgumentCaptor.forClass(ActivationToken.class);
		ArgumentCaptor<ActivationEmailRequest> mail = ArgumentCaptor.forClass(ActivationEmailRequest.class);
		InOrder order = inOrder(tokenRepository, sendActivationEmail);
		order.verify(tokenRepository).save(stored.capture());
		order.verify(sendActivationEmail).send(mail.capture());

		assertThat(stored.getValue().getUserId()).isEqualTo(userId.value());
		assertThat(stored.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
		assertThat(stored.getValue().getTokenHash()).isEqualTo(ActivationToken.hash(mail.getValue().activationToken()));
		assertThat(stored.getValue().getTokenHash()).isNotEqualTo(mail.getValue().activationToken());
		assertThat(mail.getValue().to()).isEqualTo("ana@acme.com");
		assertThat(mail.getValue().recipientName()).isEqualTo("Ana Souza");
		assertThat(mail.getValue().expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
	}

	@Test
	@DisplayName("A mail failure surfaces to the caller (the async listener logs it) after the token was stored")
	void shouldPropagateMailFailure() {
		when(tokenRepository.save(any(ActivationToken.class))).thenAnswer(call -> call.getArgument(0));
		doThrow(new IllegalStateException("queue down")).when(sendActivationEmail).send(any());

		assertThatThrownBy(() -> service().execute(command())).hasMessage("queue down");
	}

	@Test
	@DisplayName("AC7: earlier open tokens of the user are expired once the new one is stored, the new one is untouched")
	void shouldExpireEarlierOpenTokens() {
		ActivationToken previous = ActivationToken.issue(userId, NOW.minusSeconds(120)).token();
		List<ActivationToken> stored = new ArrayList<>(List.of(previous));
		when(tokenRepository.save(any(ActivationToken.class))).thenAnswer(call -> {
			ActivationToken saved = call.getArgument(0);
			if (!stored.contains(saved)) {
				stored.add(saved);
			}
			return saved;
		});
		when(tokenRepository.findUnusedByUserId(userId.value())).thenAnswer(call -> List.copyOf(stored));

		service().execute(command());

		ActivationToken current = stored.get(1);
		assertThat(previous.getExpiresAt()).isEqualTo(NOW);
		assertThat(current.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
	}
}
