package br.gravita.adapters.inbound.controllers.sales.controllers;

import br.gravita.adapters.inbound.controllers.sales.dtos.ApproveSalesOrderRequest;
import br.gravita.adapters.inbound.controllers.sales.dtos.SalesOrderResponse;
import br.gravita.core.domain.sales.SalesOrderNotFoundException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.UserNotFoundException;
import br.gravita.core.ports.inbound.sales.ApproveSalesOrderUseCase;
import br.gravita.core.ports.inbound.sales.SalesOrderView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sales/orders")
public class SalesOrderController {

	private final ApproveSalesOrderUseCase approveSalesOrderUseCase;

	@PostMapping("/{id}/approve")
	public ResponseEntity<SalesOrderResponse> approve(@PathVariable UUID id,
			@Valid @RequestBody ApproveSalesOrderRequest request) {
		SalesOrderView order = approveSalesOrderUseCase.execute(request.toCommand(id));
		return ResponseEntity.ok(SalesOrderResponse.from(order));
	}

	@ExceptionHandler(SalesOrderNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleSalesOrderNotFoundException(
			SalesOrderNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleUserNotFoundException(UserNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
