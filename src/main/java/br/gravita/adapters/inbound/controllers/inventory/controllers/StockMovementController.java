package br.gravita.adapters.inbound.controllers.inventory.controllers;

import br.gravita.adapters.dtos.response.PageResponse;
import br.gravita.adapters.inbound.controllers.inventory.dtos.RegisterStockEntryRequest;
import br.gravita.adapters.inbound.controllers.inventory.dtos.RegisterStockExitRequest;
import br.gravita.adapters.inbound.controllers.inventory.dtos.StockMovementItemResponse;
import br.gravita.adapters.inbound.controllers.inventory.dtos.StockMovementResponse;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockMovementType;
import br.gravita.core.domain.shared.PageQuery;
import br.gravita.core.ports.inbound.inventory.ListStockMovementsQuery;
import br.gravita.core.ports.inbound.inventory.ListStockMovementsUseCase;
import br.gravita.core.ports.inbound.inventory.RegisterStockEntryUseCase;
import br.gravita.core.ports.inbound.inventory.RegisterStockExitUseCase;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/inventory/movements")
public class StockMovementController {

	private final RegisterStockEntryUseCase registerStockEntryUseCase;
	private final RegisterStockExitUseCase registerStockExitUseCase;
	private final ListStockMovementsUseCase listStockMovementsUseCase;

	@PostMapping("/entry")
	public ResponseEntity<StockMovementResponse> registerEntry(@Valid @RequestBody final RegisterStockEntryRequest request) {
		final StockMovement movement = registerStockEntryUseCase.execute(request.toCommand());
		return ResponseEntity.status(HttpStatus.CREATED).body(StockMovementResponse.from(movement));
	}

	@PostMapping("/exit")
	public ResponseEntity<StockMovementResponse> registerExit(@Valid @RequestBody final RegisterStockExitRequest request) {
		final StockMovement movement = registerStockExitUseCase.execute(request.toCommand());
		return ResponseEntity.status(HttpStatus.CREATED).body(StockMovementResponse.from(movement));
	}

	/** Newest first. {@code from} and {@code to} are ISO-8601 instants, both inclusive. */
	@GetMapping
	public ResponseEntity<PageResponse<StockMovementItemResponse>> list(
			@RequestParam(required = false) final UUID productId,
			@RequestParam(required = false) final StockMovementType type,
			@RequestParam(required = false) final Instant from,
			@RequestParam(required = false) final Instant to,
			@RequestParam(defaultValue = "0") final int page,
			@RequestParam(defaultValue = "20") final int size) {
		return ResponseEntity.ok(PageResponse.from(listStockMovementsUseCase.execute(
				new ListStockMovementsQuery(productId, type, from, to, new PageQuery(page, size))),
				StockMovementItemResponse::from));
	}
}
