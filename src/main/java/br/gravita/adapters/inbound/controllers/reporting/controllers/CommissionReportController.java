package br.gravita.adapters.inbound.controllers.reporting.controllers;

import br.gravita.adapters.inbound.controllers.security.AuthenticatedUser;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.reporting.CommissionReportEntry;
import br.gravita.core.ports.inbound.reporting.CommissionReportQuery;
import br.gravita.core.ports.inbound.reporting.GetCommissionReportUseCase;
import java.time.YearMonth;
import java.util.List;
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
@RequestMapping("/api/reports/commissions")
public class CommissionReportController {

	private final GetCommissionReportUseCase getCommissionReportUseCase;

	/** {@code period} is a month as {@code yyyy-MM}; {@code salesperson} is optional and lists everybody without it. */
	@GetMapping
	public ResponseEntity<List<CommissionReportEntry>> get(@AuthenticatedUser UserId callerId,
			@RequestParam(required = false) UUID salesperson,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth period) {
		return ResponseEntity.ok(getCommissionReportUseCase.execute(new CommissionReportQuery(callerId, salesperson, period)));
	}
}
