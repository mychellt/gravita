package br.gravita.adapters.inbound.controllers.inventory.controllers;

import br.gravita.adapters.inbound.controllers.inventory.dtos.AllowNegativeStockResponse;
import br.gravita.adapters.inbound.controllers.inventory.dtos.SetAllowNegativeStockRequest;
import br.gravita.adapters.inbound.controllers.inventory.dtos.StockBalanceResponse;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.ports.inbound.inventory.GetStockBalanceQuery;
import br.gravita.core.ports.inbound.inventory.GetStockBalanceUseCase;
import br.gravita.core.ports.inbound.inventory.SetAllowNegativeStockCommand;
import br.gravita.core.ports.inbound.inventory.SetAllowNegativeStockUseCase;
import br.gravita.core.ports.inbound.inventory.StockBalanceView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/inventory/products")
public class StockBalanceController {

    private final GetStockBalanceUseCase getStockBalanceUseCase;
    private final SetAllowNegativeStockUseCase setAllowNegativeStockUseCase;

    @GetMapping("/{id}/balance")
    public ResponseEntity<StockBalanceResponse> getBalance(@PathVariable final UUID id,
                                                           @RequestParam(required = false) final UUID warehouseId) {
        final StockBalanceView view = getStockBalanceUseCase.execute(new GetStockBalanceQuery(id, warehouseId));
        return ResponseEntity.ok(StockBalanceResponse.from(view));
    }

    @PatchMapping("/{id}/allow-negative-stock")
    public ResponseEntity<AllowNegativeStockResponse> setAllowNegativeStock(@PathVariable final UUID id,
            @Valid @RequestBody final SetAllowNegativeStockRequest request) {
        final ProductDomain updated = setAllowNegativeStockUseCase
                .execute(new SetAllowNegativeStockCommand(id, request.allowNegativeStock()));
        return ResponseEntity.ok(AllowNegativeStockResponse.from(updated));
    }
}
