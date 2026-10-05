package br.gravita.adapters.inbound.controllers.inventory.controllers;

import br.gravita.adapters.inbound.controllers.inventory.dtos.StockBalanceResponse;
import br.gravita.core.ports.inbound.inventory.GetStockBalanceQuery;
import br.gravita.core.ports.inbound.inventory.GetStockBalanceUseCase;
import br.gravita.core.ports.inbound.inventory.StockBalanceView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/inventory/products")
public class StockBalanceController {

    private final GetStockBalanceUseCase getStockBalanceUseCase;

    @GetMapping("/{id}/balance")
    public ResponseEntity<StockBalanceResponse> getBalance(@PathVariable final UUID id,
                                                           @RequestParam(required = false) final UUID warehouseId) {
        final StockBalanceView view = getStockBalanceUseCase.execute(new GetStockBalanceQuery(id, warehouseId));
        return ResponseEntity.ok(StockBalanceResponse.from(view));
    }
}
