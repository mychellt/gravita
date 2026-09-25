package br.gravita.adapters.inbound.controllers.inventory.controllers;

import br.gravita.adapters.inbound.controllers.inventory.dtos.ReserveStockRequest;
import br.gravita.adapters.inbound.controllers.inventory.dtos.StockReservationResponse;
import br.gravita.core.ports.inbound.inventory.ReleaseStockReservationCommand;
import br.gravita.core.ports.inbound.inventory.ReleaseStockReservationUseCase;
import br.gravita.core.ports.inbound.inventory.ReserveStockUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/inventory/reservations")
public class StockReservationController {

	private final ReserveStockUseCase reserveStockUseCase;
	private final ReleaseStockReservationUseCase releaseStockReservationUseCase;

	@PostMapping
	public ResponseEntity<StockReservationResponse> reserve(@Valid @RequestBody ReserveStockRequest request) {
		StockReservationResponse response = StockReservationResponse.from(
				reserveStockUseCase.execute(request.toCommand()));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> release(@PathVariable UUID id) {
		releaseStockReservationUseCase.execute(new ReleaseStockReservationCommand(id));
		return ResponseEntity.noContent().build();
	}
}
