package br.gravita.adapters.inbound.controllers.reporting.controllers;

import br.gravita.adapters.inbound.controllers.security.AuthenticatedUser;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.reporting.AssessedTaxSummary;
import br.gravita.core.ports.inbound.reporting.AssessedTaxesQuery;
import br.gravita.core.ports.inbound.reporting.GetAssessedTaxesUseCase;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reports/assessed-taxes")
public class AssessedTaxesController {

	private final GetAssessedTaxesUseCase getAssessedTaxesUseCase;

	/** {@code period} is a month as {@code yyyy-MM}. */
	@GetMapping
	public ResponseEntity<AssessedTaxSummary> get(@AuthenticatedUser UserId callerId,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth period) {
		return ResponseEntity.ok(getAssessedTaxesUseCase.execute(new AssessedTaxesQuery(callerId, period)));
	}
}
