package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.CostCenterRequest;
import br.gravita.adapters.dtos.response.CostCenterResponse;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.CostCenterDomain;
import br.gravita.core.ports.business.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auxiliary/cost-centers")
public class CostCenterRestController {

    private final CreateCostCenterPort createCostCenterPort;
    private final FindCostCenterPort findCostCenterPort;
    private final ListCostCentersPort listCostCentersPort;
    private final UpdateCostCenterPort updateCostCenterPort;
    private final DeleteCostCenterPort deleteCostCenterPort;

    @PostMapping
    public ResponseEntity<CostCenterResponse> create(@Valid @RequestBody CostCenterRequest request) {
        CostCenterDomain created = createCostCenterPort.execute(new Context(request.toDomain(null)));
        return ResponseEntity.created(URI.create("/api/auxiliary/cost-centers/" + created.getId()))
                .body(CostCenterResponse.from(created));
    }

    @GetMapping
    public ResponseEntity<List<CostCenterResponse>> findAll() {
        return ResponseEntity.ok(listCostCentersPort.execute(new Context()).stream().map(CostCenterResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CostCenterResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(CostCenterResponse.from(findCostCenterPort.execute(new Context(id))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CostCenterResponse> update(@PathVariable UUID id, @Valid @RequestBody CostCenterRequest request) {
        return ResponseEntity.ok(CostCenterResponse.from(updateCostCenterPort.execute(new Context(request.toDomain(id)))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteCostCenterPort.execute(new Context(id));
        return ResponseEntity.noContent().build();
    }
}
