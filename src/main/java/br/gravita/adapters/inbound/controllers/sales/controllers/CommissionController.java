package br.gravita.adapters.inbound.controllers.sales.controllers;

import br.gravita.adapters.inbound.controllers.sales.dtos.CommissionResponse;
import br.gravita.core.domain.sales.CommissionRateNotFoundException;
import br.gravita.core.ports.inbound.sales.CalculateCommissionQuery;
import br.gravita.core.ports.inbound.sales.CalculateCommissionUseCase;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sales/commissions")
public class CommissionController {

	private final CalculateCommissionUseCase calculateCommissionUseCase;

	@GetMapping
	public ResponseEntity<List<CommissionResponse>> calculate(
			@RequestParam(required = false) final UUID salesperson,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM") final YearMonth period) {
		final List<CommissionResponse> commissions = calculateCommissionUseCase
				.execute(new CalculateCommissionQuery(salesperson, period)).stream().map(CommissionResponse::from)
				.toList();
		return ResponseEntity.ok(commissions);
	}

	@ExceptionHandler(CommissionRateNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleCommissionRateNotFoundException(
			final CommissionRateNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}
}
