package br.gravita.adapters.inbound.controllers.sales.controllers;

import br.gravita.adapters.inbound.controllers.sales.dtos.SetSalespersonTargetRequest;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.SetSalespersonTargetUseCase;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/crm/targets")
public class SalespersonTargetController {

	private final SetSalespersonTargetUseCase setSalespersonTargetUseCase;

	@PutMapping("/{salesperson}/{month}")
	public ResponseEntity<Void> set(@PathVariable UUID salesperson,
			@PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
			@Valid @RequestBody SetSalespersonTargetRequest request) {
		setSalespersonTargetUseCase.execute(request.toCommand(salesperson, month));
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
