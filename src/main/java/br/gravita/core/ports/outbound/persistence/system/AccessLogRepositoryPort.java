package br.gravita.core.ports.outbound.persistence.system;

import br.gravita.core.domain.system.AccessLog;

public interface AccessLogRepositoryPort {

	AccessLog save(AccessLog accessLog);
}
