package br.gravita.core.ports.outbound.reporting;

import br.gravita.core.domain.system.UserId;

/** What the user's profile lets them do with a reporting screen. */
public interface PermissionCheckPort {
	boolean canView(UserId userId, String screen);

	boolean canExport(UserId userId, String screen);
}
