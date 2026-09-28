package br.gravita.adapters.inbound.controllers.finance.controllers;

import br.gravita.adapters.inbound.controllers.finance.dtos.CreateManualReceivableRequest;
import br.gravita.adapters.inbound.controllers.finance.dtos.ReceivableResponse;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.CreateManualReceivableUseCase;
import jakarta.validation.Valid;
import java.net.URI;
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
@RequestMapping("/api/finance/receivables")
public class ReceivableController {

	private final CreateManualReceivableUseCase createManualReceivableUseCase;

	@PostMapping
	public ResponseEntity<ReceivableResponse> create(@Valid @RequestBody CreateManualReceivableRequest request) {
		Receivable created = createManualReceivableUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/finance/receivables/" + created.getId().value()))
				.body(ReceivableResponse.from(created));
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
