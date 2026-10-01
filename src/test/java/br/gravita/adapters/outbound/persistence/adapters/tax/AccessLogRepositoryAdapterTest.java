package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.AccessLogJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.AccessLogPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.AccessLogJpaRepository;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.usercases.system.GetAccessLogQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessLogRepositoryAdapterTest {

	@Mock
	private AccessLogJpaRepository repository;

	@Mock
	private AccessLogPersistenceMapper mapper;

	@InjectMocks
	private AccessLogRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves an access log entry")
	void shouldSaveAccessLog() {
		final AccessLog accessLog = AccessLog.login(UserId.generate(), "jane@example.com", true, "1.2.3.4", "Chrome");
		final AccessLogJpaEntity entity = AccessLogJpaEntity.builder().id(UUID.randomUUID()).build();
		when(mapper.map(accessLog)).thenReturn(entity);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity)).thenReturn(accessLog);

		final AccessLog result = adapter.save(accessLog);

		assertThat(result).isSameAs(accessLog);
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Searches the access log filtering by user and date, newest first")
	void shouldSearchFilteringByUserAndDateNewestFirst() {
		final UserId userId = UserId.generate();
		final Instant from = Instant.now().minus(365, ChronoUnit.DAYS);
		final AccessLog accessLog = AccessLog.login(userId, "jane@example.com", true, "1.2.3.4", "Chrome");
		final AccessLogJpaEntity entity = AccessLogJpaEntity.builder().id(UUID.randomUUID()).build();
		final PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp"));
		when(repository.search(userId.value(), from, null, null, null, pageable))
				.thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
		when(mapper.map(entity)).thenReturn(accessLog);

		final Page<AccessLog> result = adapter.search(new GetAccessLogQuery(userId, from, null, null, null, 0, 20));

		assertThat(result.content()).containsExactly(accessLog);
		assertThat(result.page()).isZero();
		assertThat(result.size()).isEqualTo(20);
		assertThat(result.totalElements()).isEqualTo(1);
		verify(repository).search(userId.value(), from, null, null, null, pageable);
	}

	@Test
	@DisplayName("Searches the access log without a user filter")
	void shouldSearchWithoutUserFilter() {
		final Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
		final PageRequest pageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "timestamp"));
		when(repository.search(null, from, null, "1.2.3.4", "Chrome", pageable))
				.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		final Page<AccessLog> result = adapter.search(new GetAccessLogQuery(null, from, null, "1.2.3.4", "Chrome", 1, 10));

		assertThat(result.content()).isEmpty();
		assertThat(result.page()).isEqualTo(1);
		assertThat(result.totalElements()).isZero();
	}
}
