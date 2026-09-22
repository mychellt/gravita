package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.adapters.inbound.controllers.security.AuthenticatedUser;
import br.gravita.core.domain.PermissionAction;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.usercases.system.CheckPermissionQuery;
import br.gravita.core.usercases.system.CheckPermissionUseCase;
import br.gravita.core.usercases.system.GetAccessLogQuery;
import br.gravita.core.usercases.system.GetAccessLogUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/system/access-log")
public class AccessLogController {

	private final GetAccessLogUseCase getAccessLogUseCase;
	private final CheckPermissionUseCase checkPermissionUseCase;

	public AccessLogController(GetAccessLogUseCase getAccessLogUseCase, CheckPermissionUseCase checkPermissionUseCase) {
		this.getAccessLogUseCase = getAccessLogUseCase;
		this.checkPermissionUseCase = checkPermissionUseCase;
	}

	@GetMapping
	public ResponseEntity<AccessLogPageResponse> search(
			@AuthenticatedUser UserId callerId,
			@RequestParam(required = false) UUID userId,
			@RequestParam(required = false) Instant dateFrom,
			@RequestParam(required = false) Instant dateTo,
			@RequestParam(required = false) String ip,
			@RequestParam(required = false) String device,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		if (!checkPermissionUseCase.execute(new CheckPermissionQuery(callerId, "system", "access-log", PermissionAction.VIEW))) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		Page<AccessLog> result = getAccessLogUseCase.execute(new GetAccessLogQuery(
				userId == null ? null : UserId.of(userId), dateFrom, dateTo, ip, device, page, size));
		return ResponseEntity.ok(AccessLogPageResponse.from(result));
	}
}
