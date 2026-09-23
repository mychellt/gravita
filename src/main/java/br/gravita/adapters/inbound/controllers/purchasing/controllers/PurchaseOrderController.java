package br.gravita.adapters.inbound.controllers.purchasing.controllers;

import br.gravita.adapters.inbound.controllers.purchasing.dtos.ApprovePurchaseOrderRequest;
import br.gravita.adapters.inbound.controllers.purchasing.dtos.CreatePurchaseOrderRequest;
import br.gravita.adapters.inbound.controllers.purchasing.dtos.PurchaseOrderResponse;
import br.gravita.adapters.inbound.controllers.purchasing.dtos.PurchaseReceiptResponse;
import br.gravita.adapters.inbound.controllers.purchasing.dtos.ReceivePurchaseOrderRequest;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseRequestNotFoundException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.ApprovePurchaseOrderUseCase;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseOrderUseCase;
import br.gravita.core.ports.inbound.purchasing.ReceivePurchaseOrderUseCase;
import jakarta.validation.Valid;
import java.net.URI;
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
@RequestMapping("/api/purchasing/orders")
public class PurchaseOrderController {

	private final CreatePurchaseOrderUseCase createPurchaseOrderUseCase;
	private final ReceivePurchaseOrderUseCase receivePurchaseOrderUseCase;
	private final ApprovePurchaseOrderUseCase approvePurchaseOrderUseCase;

	public PurchaseOrderController(CreatePurchaseOrderUseCase createPurchaseOrderUseCase,
			ReceivePurchaseOrderUseCase receivePurchaseOrderUseCase,
			ApprovePurchaseOrderUseCase approvePurchaseOrderUseCase) {
		this.createPurchaseOrderUseCase = createPurchaseOrderUseCase;
		this.receivePurchaseOrderUseCase = receivePurchaseOrderUseCase;
		this.approvePurchaseOrderUseCase = approvePurchaseOrderUseCase;
	}

	@PostMapping
	public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody CreatePurchaseOrderRequest request) {
		PurchaseOrderId id = createPurchaseOrderUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/purchasing/orders/" + id.value()))
				.body(PurchaseOrderResponse.from(id));
	}

	@PostMapping("/{id}/receipts")
	public ResponseEntity<PurchaseReceiptResponse> receive(@PathVariable UUID id,
			@Valid @RequestBody ReceivePurchaseOrderRequest request) {
		PurchaseReceiptId receiptId = receivePurchaseOrderUseCase.execute(request.toCommand(id));
		return ResponseEntity.created(URI.create("/api/purchasing/receipts/" + receiptId.value()))
				.body(PurchaseReceiptResponse.from(receiptId));
	}

	@PostMapping("/{id}/approve")
	public ResponseEntity<Void> approve(@PathVariable UUID id,
			@Valid @RequestBody ApprovePurchaseOrderRequest request) {
		approvePurchaseOrderUseCase.execute(request.toCommand(id));
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(PurchaseRequestNotFoundException.class)
	public ResponseEntity<Map<String, String>> handlePurchaseRequestNotFoundException(
			PurchaseRequestNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(PurchaseOrderNotFoundException.class)
	public ResponseEntity<Map<String, String>> handlePurchaseOrderNotFoundException(
			PurchaseOrderNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
