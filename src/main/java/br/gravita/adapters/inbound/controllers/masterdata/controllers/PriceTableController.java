package br.gravita.adapters.inbound.controllers.masterdata.controllers;

import br.gravita.adapters.inbound.controllers.masterdata.dtos.PriceTableResponse;
import br.gravita.adapters.inbound.controllers.masterdata.dtos.UpsertPriceTableRequest;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.masterdata.PriceTableNotFoundException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.masterdata.ManagePriceTableUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/price-tables")
public class PriceTableController {
    private final ManagePriceTableUseCase managePriceTableUseCase;

    @PostMapping
    public ResponseEntity<PriceTableResponse> create(@Valid @RequestBody final UpsertPriceTableRequest request) {
        final PriceTableId id = managePriceTableUseCase.execute(request.toCommand(null));
        return ResponseEntity.created(URI.create("/api/price-tables/" + id.value())).body(PriceTableResponse.from(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PriceTableResponse> update(@PathVariable final UUID id,
                                                     @Valid @RequestBody final UpsertPriceTableRequest request) {
        final PriceTableId saved = managePriceTableUseCase.execute(request.toCommand(id));
        return ResponseEntity.ok(PriceTableResponse.from(saved));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, String>> handleBusinessRuleException(final BusinessRuleException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(PriceTableNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePriceTableNotFoundException(final PriceTableNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }
}
