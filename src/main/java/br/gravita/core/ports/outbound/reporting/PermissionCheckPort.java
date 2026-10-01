package br.gravita.core.ports.outbound.reporting;

import br.gravita.core.domain.system.UserId;

/** Whether the user's profile lets them see a reporting screen. */
public interface PermissionCheckPort {
	boolean canView(UserId userId, String screen);
}
