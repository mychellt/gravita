package br.gravita.adapters.inbound.controllers.finance.controllers;

import br.gravita.adapters.inbound.controllers.finance.dtos.CashMovementResponse;
import br.gravita.adapters.inbound.controllers.finance.dtos.RecordInternalCashMovementRequest;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.CashMovement;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.RecordInternalCashMovementUseCase;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/finance/internal-cash")
public class InternalCashController {

	private final RecordInternalCashMovementUseCase recordInternalCashMovementUseCase;

	@PostMapping("/movements")
	public ResponseEntity<CashMovementResponse> recordMovement(
			@Valid @RequestBody RecordInternalCashMovementRequest request) {
		CashMovement recorded = recordInternalCashMovementUseCase.execute(request.toCommand());
		return ResponseEntity.status(HttpStatus.CREATED).body(CashMovementResponse.from(recorded));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
