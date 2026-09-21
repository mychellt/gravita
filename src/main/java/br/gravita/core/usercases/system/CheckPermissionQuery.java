package br.gravita.core.usercases.system;

import br.gravita.core.domain.PermissionAction;
import br.gravita.core.domain.system.UserId;

public record CheckPermissionQuery(UserId userId, String module, String screen, PermissionAction action) {
}
