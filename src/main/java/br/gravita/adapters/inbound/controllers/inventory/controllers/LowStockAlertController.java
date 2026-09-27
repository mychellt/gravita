package br.gravita.adapters.inbound.controllers.inventory.controllers;

import br.gravita.adapters.inbound.controllers.inventory.dtos.ReorderSuggestionResponse;
import br.gravita.core.ports.inbound.inventory.SuggestReorderQuery;
import br.gravita.core.ports.inbound.inventory.SuggestReorderUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/inventory/alerts")
public class LowStockAlertController {

	private final SuggestReorderUseCase suggestReorderUseCase;

	@GetMapping("/low-stock")
	public ResponseEntity<List<ReorderSuggestionResponse>> get(@RequestParam(required = false) UUID warehouseId) {
		List<ReorderSuggestionResponse> response = suggestReorderUseCase.execute(new SuggestReorderQuery(warehouseId))
				.stream()
				.map(ReorderSuggestionResponse::from)
				.toList();
		return ResponseEntity.ok(response);
	}
}
