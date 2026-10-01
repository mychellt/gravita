package br.gravita.adapters.inbound.controllers.reporting.controllers;

import br.gravita.adapters.inbound.controllers.security.AuthenticatedUser;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.reporting.GetSupplierPurchaseSummaryUseCase;
import br.gravita.core.ports.inbound.reporting.SupplierPurchaseSummary;
import br.gravita.core.ports.inbound.reporting.SupplierPurchaseSummaryQuery;
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
@RequestMapping("/api/reports/purchases-by-supplier")
public class SupplierPurchaseSummaryController {

	private final GetSupplierPurchaseSummaryUseCase getSupplierPurchaseSummaryUseCase;

	/** {@code period} is a month as {@code yyyy-MM}. */
	@GetMapping
	public ResponseEntity<List<SupplierPurchaseSummary>> get(@AuthenticatedUser UserId callerId,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth period,
			@RequestParam(required = false) UUID companyId) {
		return ResponseEntity.ok(getSupplierPurchaseSummaryUseCase
				.execute(new SupplierPurchaseSummaryQuery(callerId, period, companyId)));
	}
}
