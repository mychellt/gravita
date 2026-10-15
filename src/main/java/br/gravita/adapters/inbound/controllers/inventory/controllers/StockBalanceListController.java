package br.gravita.adapters.inbound.controllers.inventory.controllers;

import br.gravita.adapters.dtos.response.PageResponse;
import br.gravita.adapters.inbound.controllers.inventory.dtos.StockBalanceItemResponse;
import br.gravita.core.domain.shared.PageQuery;
import br.gravita.core.ports.inbound.inventory.ListStockBalancesQuery;
import br.gravita.core.ports.inbound.inventory.ListStockBalancesUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/inventory/balances")
public class StockBalanceListController {

	private final ListStockBalancesUseCase listStockBalancesUseCase;

	@GetMapping
	public ResponseEntity<PageResponse<StockBalanceItemResponse>> list(
			@RequestParam(required = false) final UUID productId,
			@RequestParam(defaultValue = "0") final int page,
			@RequestParam(defaultValue = "20") final int size) {
		return ResponseEntity.ok(PageResponse.from(
				listStockBalancesUseCase.execute(new ListStockBalancesQuery(productId, new PageQuery(page, size))),
				StockBalanceItemResponse::from));
	}
}
