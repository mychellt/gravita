package br.gravita.adapters.inbound.controllers.purchasing.controllers;

import br.gravita.adapters.inbound.controllers.purchasing.dtos.CreatePurchaseOrderRequest;
import br.gravita.adapters.inbound.controllers.purchasing.dtos.PurchaseOrderResponse;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseRequestNotFoundException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseOrderUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchasing/orders")
public class PurchaseOrderController {

	private final CreatePurchaseOrderUseCase createPurchaseOrderUseCase;

	public PurchaseOrderController(CreatePurchaseOrderUseCase createPurchaseOrderUseCase) {
		this.createPurchaseOrderUseCase = createPurchaseOrderUseCase;
	}

	@PostMapping
	public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody CreatePurchaseOrderRequest request) {
		PurchaseOrderId id = createPurchaseOrderUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/purchasing/orders/" + id.value()))
				.body(PurchaseOrderResponse.from(id));
	}

	@ExceptionHandler(PurchaseRequestNotFoundException.class)
	public ResponseEntity<Map<String, String>> handlePurchaseRequestNotFoundException(
			PurchaseRequestNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
