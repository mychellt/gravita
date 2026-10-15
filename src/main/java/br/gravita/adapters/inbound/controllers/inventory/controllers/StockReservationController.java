package br.gravita.adapters.inbound.controllers.inventory.controllers;

import br.gravita.adapters.dtos.response.PageResponse;
import br.gravita.adapters.inbound.controllers.inventory.dtos.ReserveStockRequest;
import br.gravita.adapters.inbound.controllers.inventory.dtos.StockReservationItemResponse;
import br.gravita.adapters.inbound.controllers.inventory.dtos.StockReservationResponse;
import br.gravita.core.domain.inventory.StockReservationStatus;
import br.gravita.core.domain.shared.PageQuery;
import br.gravita.core.ports.inbound.inventory.ListStockReservationsQuery;
import br.gravita.core.ports.inbound.inventory.ListStockReservationsUseCase;
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
	private final ListStockReservationsUseCase listStockReservationsUseCase;

	@PostMapping
	public ResponseEntity<StockReservationResponse> reserve(@Valid @RequestBody final ReserveStockRequest request) {
		final StockReservationResponse response = StockReservationResponse.from(
				reserveStockUseCase.execute(request.toCommand()));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> release(@PathVariable final UUID id) {
		releaseStockReservationUseCase.execute(new ReleaseStockReservationCommand(id));
		return ResponseEntity.noContent().build();
	}

	@GetMapping
	public ResponseEntity<PageResponse<StockReservationItemResponse>> list(
			@RequestParam(required = false) final StockReservationStatus status,
			@RequestParam(required = false) final UUID productId,
			@RequestParam(defaultValue = "0") final int page,
			@RequestParam(defaultValue = "20") final int size) {
		return ResponseEntity.ok(PageResponse.from(listStockReservationsUseCase.execute(
				new ListStockReservationsQuery(status, productId, new PageQuery(page, size))),
				StockReservationItemResponse::from));
	}
}
