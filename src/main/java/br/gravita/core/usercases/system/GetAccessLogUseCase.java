package br.gravita.core.usercases.system;

import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.system.AccessLog;

public interface GetAccessLogUseCase {
	Page<AccessLog> execute(GetAccessLogQuery query);
}
