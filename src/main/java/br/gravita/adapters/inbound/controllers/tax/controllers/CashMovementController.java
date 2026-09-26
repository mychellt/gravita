package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.RecordCashMovementRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.RecordCashMovementResponse;
import br.gravita.core.domain.tax.CashMovementId;
import br.gravita.core.ports.inbound.tax.RecordCashMovementUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/pdv/cash-movements")
public class CashMovementController {

	private final RecordCashMovementUseCase recordCashMovementUseCase;

	@PostMapping
	public ResponseEntity<RecordCashMovementResponse> record(@Valid @RequestBody RecordCashMovementRequest request) {
		CashMovementId id = recordCashMovementUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/pdv/cash-movements/" + id.value()))
				.body(RecordCashMovementResponse.from(id));
	}
}
