package br.gravita.adapters.inbound.controllers.inventory.controllers;

import br.gravita.adapters.inbound.controllers.inventory.dtos.RegisterStockEntryRequest;
import br.gravita.adapters.inbound.controllers.inventory.dtos.RegisterStockExitRequest;
import br.gravita.adapters.inbound.controllers.inventory.dtos.StockMovementResponse;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.ports.inbound.inventory.RegisterStockEntryUseCase;
import br.gravita.core.ports.inbound.inventory.RegisterStockExitUseCase;
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
@RequestMapping("/api/inventory/movements")
public class StockMovementController {

	private final RegisterStockEntryUseCase registerStockEntryUseCase;
	private final RegisterStockExitUseCase registerStockExitUseCase;

	@PostMapping("/entry")
	public ResponseEntity<StockMovementResponse> registerEntry(@Valid @RequestBody RegisterStockEntryRequest request) {
		StockMovement movement = registerStockEntryUseCase.execute(request.toCommand());
		return ResponseEntity.status(HttpStatus.CREATED).body(StockMovementResponse.from(movement));
	}

	@PostMapping("/exit")
	public ResponseEntity<StockMovementResponse> registerExit(@Valid @RequestBody RegisterStockExitRequest request) {
		StockMovement movement = registerStockExitUseCase.execute(request.toCommand());
		return ResponseEntity.status(HttpStatus.CREATED).body(StockMovementResponse.from(movement));
	}
}
