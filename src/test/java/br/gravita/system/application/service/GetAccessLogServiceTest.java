package br.gravita.system.application.service;

import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.persistence.system.AccessLogRepositoryPort;
import br.gravita.core.usercases.system.GetAccessLogQuery;
import br.gravita.core.usercases.tax.GetAccessLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAccessLogServiceTest {

	@Mock
	private AccessLogRepositoryPort accessLogRepositoryPort;

	private GetAccessLogService service() {
		return new GetAccessLogService(accessLogRepositoryPort);
	}

	@Test
	@DisplayName("Clamps dateFrom to the retention cutoff when none is given")
	void shouldClampDateFromToTheRetentionCutoffWhenNoneIsGiven() {
		when(accessLogRepositoryPort.search(any())).thenReturn(new Page<>(List.of(), 0, 20, 0));

		service().execute(new GetAccessLogQuery(null, null, null, null, null, 0, 20));

		final ArgumentCaptor<GetAccessLogQuery> captor = ArgumentCaptor.forClass(GetAccessLogQuery.class);
		verify(accessLogRepositoryPort).search(captor.capture());
		final Instant expectedCutoff = Instant.now().minus(365, ChronoUnit.DAYS);
		assertThat(captor.getValue().dateFrom()).isCloseTo(expectedCutoff, within(Duration.ofMinutes(1)));
	}

	@Test
	@DisplayName("Clamps dateFrom to the retention cutoff when the requested date is older")
	void shouldClampDateFromToTheRetentionCutoffWhenARequestedDateIsOlder() {
		when(accessLogRepositoryPort.search(any())).thenReturn(new Page<>(List.of(), 0, 20, 0));
		final Instant thirteenMonthsAgo = Instant.now().minus(395, ChronoUnit.DAYS);

		service().execute(new GetAccessLogQuery(null, thirteenMonthsAgo, null, null, null, 0, 20));

		final ArgumentCaptor<GetAccessLogQuery> captor = ArgumentCaptor.forClass(GetAccessLogQuery.class);
		verify(accessLogRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().dateFrom()).isAfter(thirteenMonthsAgo);
	}

	@Test
	@DisplayName("Keeps the requested dateFrom when it is within the retention window")
	void shouldKeepARequestedDateFromWhenItIsWithinTheRetentionWindow() {
		when(accessLogRepositoryPort.search(any())).thenReturn(new Page<>(List.of(), 0, 20, 0));
		final Instant oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS);

		service().execute(new GetAccessLogQuery(null, oneWeekAgo, null, null, null, 0, 20));

		final ArgumentCaptor<GetAccessLogQuery> captor = ArgumentCaptor.forClass(GetAccessLogQuery.class);
		verify(accessLogRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().dateFrom()).isEqualTo(oneWeekAgo);
	}

	@Test
	@DisplayName("Passes the user id and other filters through unchanged")
	void shouldPassUserIdAndOtherFiltersThroughUnchanged() {
		final UserId userId = UserId.generate();
		when(accessLogRepositoryPort.search(any())).thenReturn(new Page<>(List.of(), 0, 20, 0));

		service().execute(new GetAccessLogQuery(userId, null, null, "1.2.3.4", "Chrome", 1, 10));

		final ArgumentCaptor<GetAccessLogQuery> captor = ArgumentCaptor.forClass(GetAccessLogQuery.class);
		verify(accessLogRepositoryPort).search(captor.capture());
		assertThat(captor.getValue().userId()).isEqualTo(userId);
		assertThat(captor.getValue().ip()).isEqualTo("1.2.3.4");
		assertThat(captor.getValue().device()).isEqualTo("Chrome");
		assertThat(captor.getValue().page()).isEqualTo(1);
		assertThat(captor.getValue().size()).isEqualTo(10);
	}

	@Test
	@DisplayName("Returns whatever page the repository produces")
	void shouldReturnWhateverPageTheRepositoryProduces() {
		final AccessLog entry = AccessLog.login(UserId.generate(), "jane@example.com", true, "1.2.3.4", "Chrome");
		final Page<AccessLog> repositoryPage = new Page<>(List.of(entry), 0, 20, 1);
		when(accessLogRepositoryPort.search(any())).thenReturn(repositoryPage);

		final Page<AccessLog> result = service().execute(new GetAccessLogQuery(null, null, null, null, null, 0, 20));

		assertThat(result).isSameAs(repositoryPage);
	}
}
