package br.gravita.adapters.inbound.controllers.inventory.controllers;

import br.gravita.adapters.inbound.controllers.inventory.dtos.ExpiringLotResponse;
import br.gravita.core.ports.inbound.inventory.CheckExpiringLotsQuery;
import br.gravita.core.ports.inbound.inventory.CheckExpiringLotsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/inventory/alerts")
public class ExpiringLotsController {

	private final CheckExpiringLotsUseCase checkExpiringLotsUseCase;

	@GetMapping("/expiring-lots")
	public ResponseEntity<List<ExpiringLotResponse>> get(@RequestParam int withinDays,
			@RequestParam(required = false) UUID warehouseId) {
		List<ExpiringLotResponse> response = checkExpiringLotsUseCase.execute(new CheckExpiringLotsQuery(withinDays, warehouseId))
				.stream()
				.map(ExpiringLotResponse::from)
				.toList();
		return ResponseEntity.ok(response);
	}
}
