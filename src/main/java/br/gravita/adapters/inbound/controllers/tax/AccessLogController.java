package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.system.AccessLog;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.usercases.system.GetAccessLogQuery;
import br.gravita.core.usercases.system.GetAccessLogUseCase;
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

	public AccessLogController(GetAccessLogUseCase getAccessLogUseCase) {
		this.getAccessLogUseCase = getAccessLogUseCase;
	}

	@GetMapping
	public ResponseEntity<AccessLogPageResponse> search(
			@RequestParam(required = false) UUID userId,
			@RequestParam(required = false) Instant dateFrom,
			@RequestParam(required = false) Instant dateTo,
			@RequestParam(required = false) String ip,
			@RequestParam(required = false) String device,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		Page<AccessLog> result = getAccessLogUseCase.execute(new GetAccessLogQuery(
				userId == null ? null : UserId.of(userId), dateFrom, dateTo, ip, device, page, size));
		return ResponseEntity.ok(AccessLogPageResponse.from(result));
	}
}
