package br.gravita.adapters.inbound.controllers.inventory.controllers;

import br.gravita.core.ports.inbound.inventory.GetInventorySummaryUseCase;
import br.gravita.core.ports.inbound.inventory.InventorySummary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/inventory/summary")
public class InventorySummaryController {

	private final GetInventorySummaryUseCase getInventorySummaryUseCase;

	@GetMapping
	public InventorySummary get() {
		return getInventorySummaryUseCase.execute();
	}
}
