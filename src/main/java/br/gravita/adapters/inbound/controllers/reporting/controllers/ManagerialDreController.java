package br.gravita.adapters.inbound.controllers.reporting.controllers;

import br.gravita.adapters.inbound.controllers.security.AuthenticatedUser;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.reporting.DreQuery;
import br.gravita.core.ports.inbound.reporting.GetManagerialDreUseCase;
import br.gravita.core.ports.inbound.reporting.ManagerialDre;
import java.time.YearMonth;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reports/dre")
public class ManagerialDreController {

	private final GetManagerialDreUseCase getManagerialDreUseCase;

	/** {@code period} is a month as {@code yyyy-MM}; {@code costCenter} optionally narrows the expenses. */
	@GetMapping
	public ResponseEntity<ManagerialDre> get(@AuthenticatedUser UserId callerId,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth period,
			@RequestParam(required = false) UUID costCenter, @RequestParam(required = false) UUID companyId) {
		return ResponseEntity.ok(getManagerialDreUseCase.execute(new DreQuery(callerId, period, costCenter, companyId)));
	}
}
