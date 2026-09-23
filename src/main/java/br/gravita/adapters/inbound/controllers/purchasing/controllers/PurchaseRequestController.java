package br.gravita.adapters.inbound.controllers.purchasing.controllers;

import br.gravita.adapters.inbound.controllers.purchasing.dtos.CreatePurchaseRequestRequest;
import br.gravita.adapters.inbound.controllers.purchasing.dtos.PurchaseRequestResponse;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseRequestUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchasing/requests")
public class PurchaseRequestController {

	private final CreatePurchaseRequestUseCase createPurchaseRequestUseCase;

	public PurchaseRequestController(CreatePurchaseRequestUseCase createPurchaseRequestUseCase) {
		this.createPurchaseRequestUseCase = createPurchaseRequestUseCase;
	}

	@PostMapping
	public ResponseEntity<PurchaseRequestResponse> create(@Valid @RequestBody CreatePurchaseRequestRequest request) {
		PurchaseRequestId id = createPurchaseRequestUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/purchasing/requests/" + id.value()))
				.body(PurchaseRequestResponse.from(id));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
