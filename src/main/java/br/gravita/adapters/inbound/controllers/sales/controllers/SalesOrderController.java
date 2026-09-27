package br.gravita.adapters.inbound.controllers.sales.controllers;

import br.gravita.adapters.inbound.controllers.sales.dtos.CancelSalesOrderRequest;
import br.gravita.core.domain.sales.SalesOrderNotFoundException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.CancelSalesOrderUseCase;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sales/orders")
public class SalesOrderController {

	private final CancelSalesOrderUseCase cancelSalesOrderUseCase;

	@PostMapping("/{id}/cancel")
	public ResponseEntity<Void> cancel(@PathVariable UUID id, @RequestBody CancelSalesOrderRequest request) {
		cancelSalesOrderUseCase.execute(request.toCommand(id));
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(SalesOrderNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleSalesOrderNotFoundException(
			SalesOrderNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
