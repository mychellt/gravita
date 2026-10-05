package br.gravita.adapters.inbound.controllers.inventory.controllers;

import br.gravita.adapters.inbound.controllers.inventory.dtos.ConfirmTransferRequest;
import br.gravita.adapters.inbound.controllers.inventory.dtos.InitiateTransferRequest;
import br.gravita.adapters.inbound.controllers.inventory.dtos.StockMovementResponse;
import br.gravita.core.ports.inbound.inventory.TransferStockUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/inventory/transfers")
public class StockTransferController {

	private final TransferStockUseCase transferStockUseCase;

	@PostMapping
	public ResponseEntity<StockMovementResponse> initiate(@Valid @RequestBody final InitiateTransferRequest request) {
		final StockMovementResponse response = StockMovementResponse.from(transferStockUseCase.initiate(request.toCommand()));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/{id}/confirm")
	public ResponseEntity<StockMovementResponse> confirm(@PathVariable final UUID id,
			@Valid @RequestBody final ConfirmTransferRequest request) {
		final StockMovementResponse response = StockMovementResponse
				.from(transferStockUseCase.confirm(request.toCommand(id)));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
