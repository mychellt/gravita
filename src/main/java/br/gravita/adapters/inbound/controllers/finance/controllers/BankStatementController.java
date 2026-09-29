package br.gravita.adapters.inbound.controllers.finance.controllers;

import br.gravita.adapters.inbound.controllers.finance.dtos.ReconcileBankStatementRequest;
import br.gravita.adapters.inbound.controllers.finance.dtos.ReconciliationResponse;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.ReconcileBankStatementUseCase;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/finance/bank-statements")
public class BankStatementController {

	private final ReconcileBankStatementUseCase reconcileBankStatementUseCase;

	@PostMapping("/import")
	public ResponseEntity<ReconciliationResponse> importStatement(
			@Valid @RequestBody ReconcileBankStatementRequest request) {
		return ResponseEntity
				.ok(ReconciliationResponse.from(reconcileBankStatementUseCase.execute(request.toCommand())));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
