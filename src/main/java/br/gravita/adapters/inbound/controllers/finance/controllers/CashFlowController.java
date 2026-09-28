package br.gravita.adapters.inbound.controllers.finance.controllers;

import br.gravita.adapters.inbound.controllers.finance.dtos.CashFlowResponse;
import br.gravita.core.domain.finance.CashFlowGranularity;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.GetCashFlowQuery;
import br.gravita.core.ports.inbound.finance.GetCashFlowUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/finance/cash-flow")
public class CashFlowController {

	private final GetCashFlowUseCase getCashFlowUseCase;

	@GetMapping
	public ResponseEntity<CashFlowResponse> get(
			@RequestParam(defaultValue = "DAILY") CashFlowGranularity granularity,
			@RequestParam(required = false) UUID companyId, @RequestParam(required = false) UUID branchId,
			@RequestParam(required = false) UUID bankAccountId, @RequestParam(required = false) UUID costCenterId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) BigDecimal openingBalance) {
		return ResponseEntity.ok(CashFlowResponse.from(getCashFlowUseCase.execute(new GetCashFlowQuery(granularity,
				companyId, branchId, bankAccountId, costCenterId, from, to, openingBalance))));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
