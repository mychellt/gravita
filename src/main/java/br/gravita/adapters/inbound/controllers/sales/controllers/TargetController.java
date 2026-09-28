package br.gravita.adapters.inbound.controllers.sales.controllers;

import br.gravita.adapters.inbound.controllers.sales.dtos.TargetProgressResponse;
import br.gravita.core.ports.inbound.sales.GetTargetProgressQuery;
import br.gravita.core.ports.inbound.sales.GetTargetProgressUseCase;
import java.time.YearMonth;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/crm/targets")
public class TargetController {

	private final GetTargetProgressUseCase getTargetProgressUseCase;

	@GetMapping("/{salesperson}/{month}")
	public ResponseEntity<TargetProgressResponse> getProgress(@PathVariable UUID salesperson,
			@PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
		TargetProgressResponse response = TargetProgressResponse
				.from(getTargetProgressUseCase.execute(new GetTargetProgressQuery(salesperson, month)));
		return ResponseEntity.ok(response);
	}
}
