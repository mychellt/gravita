package br.gravita.adapters.inbound.controllers.purchasing.controllers;

import br.gravita.core.domain.purchasing.PurchaseOrderNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReceiptNotFoundException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.ConfirmPurchaseReceiptCommand;
import br.gravita.core.ports.inbound.purchasing.ConfirmPurchaseReceiptUseCase;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchasing/receipts")
public class PurchaseReceiptController {

	private final ConfirmPurchaseReceiptUseCase confirmPurchaseReceiptUseCase;

	public PurchaseReceiptController(ConfirmPurchaseReceiptUseCase confirmPurchaseReceiptUseCase) {
		this.confirmPurchaseReceiptUseCase = confirmPurchaseReceiptUseCase;
	}

	@PostMapping("/{id}/confirm")
	public ResponseEntity<Void> confirm(@PathVariable UUID id) {
		confirmPurchaseReceiptUseCase.execute(new ConfirmPurchaseReceiptCommand(PurchaseReceiptId.of(id)));
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(PurchaseReceiptNotFoundException.class)
	public ResponseEntity<Map<String, String>> handlePurchaseReceiptNotFoundException(
			PurchaseReceiptNotFoundException exception) {
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
