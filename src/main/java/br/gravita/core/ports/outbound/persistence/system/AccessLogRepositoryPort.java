package br.gravita.core.ports.outbound.persistence.system;

import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.usercases.system.GetAccessLogQuery;

public interface AccessLogRepositoryPort {

	AccessLog save(AccessLog accessLog);

	Page<AccessLog> search(GetAccessLogQuery query);
}
