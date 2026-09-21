package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.mappers.tax.AccessLogPersistenceMapperImpl;
import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.domain.system.AccessLogEvent;
import br.gravita.core.domain.system.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

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
}
