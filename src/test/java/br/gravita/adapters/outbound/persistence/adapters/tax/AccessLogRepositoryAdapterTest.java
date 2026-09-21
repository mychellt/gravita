package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.mappers.tax.AccessLogPersistenceMapperImpl;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.domain.system.AccessLogEvent;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.usercases.system.GetAccessLogQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({AccessLogRepositoryAdapter.class, AccessLogPersistenceMapperImpl.class})
class AccessLogRepositoryAdapterTest {

	@Autowired
	private AccessLogRepositoryAdapter repositoryAdapter;

	@Test
	void shouldPersistASuccessfulLoginWithTheAttemptingUser() {
		AccessLog accessLog = AccessLog.login(UserId.generate(), "jane@example.com", true, "1.2.3.4", "Chrome");

		AccessLog saved = repositoryAdapter.save(accessLog);

		assertThat(saved.getEvent()).isEqualTo(AccessLogEvent.LOGIN);
		assertThat(saved.isSuccessful()).isTrue();
		assertThat(saved.getUserId()).isEqualTo(accessLog.getUserId());
		assertThat(saved.getIp()).isEqualTo("1.2.3.4");
		assertThat(saved.getDevice()).isEqualTo("Chrome");
	}

	@Test
	void shouldPersistAFailedLoginWithNoMatchingUser() {
		AccessLog accessLog = AccessLog.login(null, "ghost@example.com", false, "5.6.7.8", "curl/8.0");

		AccessLog saved = repositoryAdapter.save(accessLog);

		assertThat(saved.getUserId()).isNull();
		assertThat(saved.getEmail()).isEqualTo("ghost@example.com");
		assertThat(saved.isSuccessful()).isFalse();
	}

	@Test
	void shouldExcludeEntriesOlderThanTheGivenDateFrom() {
		UserId userId = UserId.generate();
		Instant now = Instant.now();
		repositoryAdapter.save(AccessLog.builder()
				.id(UUID.randomUUID()).userId(userId).email("jane@example.com")
				.event(AccessLogEvent.LOGIN).successful(true).ip("1.2.3.4").device("Chrome")
				.timestamp(now.minus(400, ChronoUnit.DAYS)).build());
		AccessLog recent = repositoryAdapter.save(AccessLog.builder()
				.id(UUID.randomUUID()).userId(userId).email("jane@example.com")
				.event(AccessLogEvent.LOGIN).successful(true).ip("1.2.3.4").device("Chrome")
				.timestamp(now.minus(1, ChronoUnit.DAYS)).build());

		Page<AccessLog> result = repositoryAdapter.search(
				new GetAccessLogQuery(null, now.minus(365, ChronoUnit.DAYS), null, null, null, 0, 20));

		assertThat(result.content()).extracting(AccessLog::getId).containsExactly(recent.getId());
		assertThat(result.totalElements()).isEqualTo(1);
	}

	@Test
	void shouldFilterByUserId() {
		UserId targetUser = UserId.generate();
		Instant now = Instant.now();
		AccessLog targetEntry = repositoryAdapter.save(AccessLog.builder()
				.id(UUID.randomUUID()).userId(targetUser).email("jane@example.com")
				.event(AccessLogEvent.LOGIN).successful(true).ip("1.2.3.4").device("Chrome")
				.timestamp(now).build());
		repositoryAdapter.save(AccessLog.builder()
				.id(UUID.randomUUID()).userId(UserId.generate()).email("john@example.com")
				.event(AccessLogEvent.LOGIN).successful(true).ip("5.6.7.8").device("Firefox")
				.timestamp(now).build());

		Page<AccessLog> result = repositoryAdapter.search(
				new GetAccessLogQuery(targetUser, now.minus(1, ChronoUnit.DAYS), null, null, null, 0, 20));

		assertThat(result.content()).extracting(AccessLog::getId).containsExactly(targetEntry.getId());
	}
}
