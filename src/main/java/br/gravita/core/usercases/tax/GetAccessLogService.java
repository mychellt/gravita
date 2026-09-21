package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.ports.outbound.persistence.system.AccessLogRepositoryPort;
import br.gravita.core.usercases.system.GetAccessLogQuery;
import br.gravita.core.usercases.system.GetAccessLogUseCase;

import java.time.Instant;
import java.time.ZoneOffset;

@UseCase
public class GetAccessLogService implements GetAccessLogUseCase {

	private final AccessLogRepositoryPort accessLogRepositoryPort;

	public GetAccessLogService(AccessLogRepositoryPort accessLogRepositoryPort) {
		this.accessLogRepositoryPort = accessLogRepositoryPort;
	}

	@Override
	public Page<AccessLog> execute(GetAccessLogQuery query) {
		Instant retentionCutoff = Instant.now().atZone(ZoneOffset.UTC).minusMonths(AccessLog.RETENTION_MONTHS).toInstant();
		Instant effectiveDateFrom = query.dateFrom() == null || query.dateFrom().isBefore(retentionCutoff)
				? retentionCutoff
				: query.dateFrom();

		GetAccessLogQuery effectiveQuery = new GetAccessLogQuery(
				query.userId(), effectiveDateFrom, query.dateTo(), query.ip(), query.device(), query.page(), query.size());

		return accessLogRepositoryPort.search(effectiveQuery);
	}
}
