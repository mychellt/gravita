package br.gravita.adapters.inbound.controllers.sales.controllers;

import br.gravita.adapters.inbound.controllers.sales.dtos.SetSalespersonTargetRequest;
import br.gravita.adapters.inbound.controllers.sales.dtos.TargetProgressResponse;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.GetTargetProgressQuery;
import br.gravita.core.ports.inbound.sales.GetTargetProgressUseCase;
import br.gravita.core.ports.inbound.sales.SetSalespersonTargetUseCase;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
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
	private final GetTargetProgressUseCase getTargetProgressUseCase;

	@PutMapping("/{salesperson}/{month}")
	public ResponseEntity<Void> set(@PathVariable final UUID salesperson,
			@PathVariable @DateTimeFormat(pattern = "yyyy-MM") final YearMonth month,
			@Valid @RequestBody final SetSalespersonTargetRequest request) {
		setSalespersonTargetUseCase.execute(request.toCommand(salesperson, month));
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{salesperson}/{month}")
	public ResponseEntity<TargetProgressResponse> getProgress(@PathVariable final UUID salesperson,
			@PathVariable @DateTimeFormat(pattern = "yyyy-MM") final YearMonth month) {
		final TargetProgressResponse response = TargetProgressResponse
				.from(getTargetProgressUseCase.execute(new GetTargetProgressQuery(salesperson, month)));
		return ResponseEntity.ok(response);
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(final BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
