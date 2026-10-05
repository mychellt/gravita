package br.gravita.adapters.inbound.controllers.inventory.controllers;

import br.gravita.adapters.inbound.controllers.inventory.dtos.AdjustInventoryRequest;
import br.gravita.adapters.inbound.controllers.inventory.dtos.StockMovementResponse;
import br.gravita.core.ports.inbound.inventory.AdjustInventoryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/inventory/adjustments")
public class StockAdjustmentController {

	private final AdjustInventoryUseCase adjustInventoryUseCase;

	@PostMapping
	public ResponseEntity<StockMovementResponse> adjust(@Valid @RequestBody final AdjustInventoryRequest request) {
		final StockMovementResponse response = StockMovementResponse.from(adjustInventoryUseCase.execute(request.toCommand()));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
