package br.gravita.adapters.outbound.reporting;

import br.gravita.core.domain.PermissionAction;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.usercases.system.CheckPermissionQuery;
import br.gravita.core.usercases.system.CheckPermissionUseCase;
import org.springframework.stereotype.Component;

/** Reporting screens are the {@code reporting} module's permissions in the user's profile. */
@Component
class PermissionCheckAdapter implements PermissionCheckPort {

	static final String MODULE = "reporting";

	private final CheckPermissionUseCase checkPermissionUseCase;

	PermissionCheckAdapter(CheckPermissionUseCase checkPermissionUseCase) {
		this.checkPermissionUseCase = checkPermissionUseCase;
	}

	@Override
	public boolean canView(UserId userId, String screen) {
		return can(userId, screen, PermissionAction.VIEW);
	}

	@Override
	public boolean canExport(UserId userId, String screen) {
		return can(userId, screen, PermissionAction.EXPORT);
	}

	private boolean can(UserId userId, String screen, PermissionAction action) {
		return checkPermissionUseCase.execute(new CheckPermissionQuery(userId, MODULE, screen, action));
	}
}
