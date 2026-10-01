package br.gravita.adapters.inbound.controllers.reporting.controllers;

import br.gravita.adapters.inbound.controllers.security.AuthenticatedUser;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.reporting.DashboardPeriod;
import br.gravita.core.ports.inbound.reporting.DashboardQuery;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView;
import br.gravita.core.ports.inbound.reporting.GetExecutiveDashboardUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reports/dashboard")
public class ExecutiveDashboardController {

	private final GetExecutiveDashboardUseCase getExecutiveDashboardUseCase;

	@GetMapping
	public ResponseEntity<ExecutiveDashboardView> get(@AuthenticatedUser UserId callerId,
			@RequestParam(defaultValue = "MONTH") DashboardPeriod period,
			@RequestParam(required = false) UUID companyId) {
		return ResponseEntity.ok(getExecutiveDashboardUseCase.execute(new DashboardQuery(callerId, period, companyId)));
	}
}
