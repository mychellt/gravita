package br.gravita.adapters.inbound.controllers.finance.controllers;

import br.gravita.adapters.inbound.controllers.finance.dtos.CreateManualPayableRequest;
import br.gravita.adapters.inbound.controllers.finance.dtos.PayableResponse;
import br.gravita.adapters.inbound.controllers.finance.dtos.SplitPayableRequest;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.CreateManualPayableUseCase;
import br.gravita.core.ports.inbound.finance.SplitPayableByCostCenterUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/finance/payables")
public class PayableController {

	private final CreateManualPayableUseCase createManualPayableUseCase;
	private final SplitPayableByCostCenterUseCase splitPayableByCostCenterUseCase;

	@PostMapping
	public ResponseEntity<PayableResponse> create(@Valid @RequestBody CreateManualPayableRequest request) {
		Payable created = createManualPayableUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/finance/payables/" + created.getId().value()))
				.body(PayableResponse.from(created));
	}

	@PatchMapping("/{id}")
	public PayableResponse split(@PathVariable UUID id, @Valid @RequestBody SplitPayableRequest request) {
		return PayableResponse.from(splitPayableByCostCenterUseCase.execute(request.toCommand(id)));
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
