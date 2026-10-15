package br.gravita.adapters.inbound.controllers.inventory.controllers;

import br.gravita.adapters.dtos.response.PageResponse;
import br.gravita.adapters.inbound.controllers.inventory.dtos.StockUnitResponse;
import br.gravita.core.domain.shared.PageQuery;
import br.gravita.core.ports.inbound.inventory.ListLotsQuery;
import br.gravita.core.ports.inbound.inventory.ListLotsUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/inventory/lots")
public class StockLotController {

	private final ListLotsUseCase listLotsUseCase;

	/** {@code kind} is LOT (the default) or SERIAL, to list serial units instead of lots. */
	@GetMapping
	public ResponseEntity<PageResponse<StockUnitResponse>> list(
			@RequestParam(required = false) final UUID productId,
			@RequestParam(required = false) final ListLotsQuery.Kind kind,
			@RequestParam(defaultValue = "0") final int page,
			@RequestParam(defaultValue = "20") final int size) {
		return ResponseEntity.ok(PageResponse.from(
				listLotsUseCase.execute(new ListLotsQuery(productId, kind, new PageQuery(page, size))),
				StockUnitResponse::from));
	}
}
