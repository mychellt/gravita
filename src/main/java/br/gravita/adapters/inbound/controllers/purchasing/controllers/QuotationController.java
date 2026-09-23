package br.gravita.adapters.inbound.controllers.purchasing.controllers;

import br.gravita.adapters.inbound.controllers.purchasing.dtos.RegisterQuotationResponseRequest;
import br.gravita.core.domain.purchasing.QuotationNotFoundException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.RegisterQuotationResponseUseCase;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchasing/quotations")
public class QuotationController {

	private final RegisterQuotationResponseUseCase registerQuotationResponseUseCase;

	public QuotationController(RegisterQuotationResponseUseCase registerQuotationResponseUseCase) {
		this.registerQuotationResponseUseCase = registerQuotationResponseUseCase;
	}

	@PostMapping("/{id}/responses")
	public ResponseEntity<Void> registerResponse(@PathVariable UUID id,
			@Valid @RequestBody RegisterQuotationResponseRequest request) {
		registerQuotationResponseUseCase.execute(request.toCommand(id));
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(QuotationNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleQuotationNotFoundException(QuotationNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
