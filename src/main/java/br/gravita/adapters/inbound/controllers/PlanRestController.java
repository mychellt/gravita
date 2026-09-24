package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.PlanRequest;
import br.gravita.adapters.dtos.response.PlanResponse;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.PlanDomain;
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
@RequestMapping("/api/plans")
public class PlanRestController {

    private final CreatePlanPort createPlanPort;
    private final FindPlanPort findPlanPort;
    private final ListPlansPort listPlansPort;
    private final UpdatePlanPort updatePlanPort;
    private final DeletePlanPort deletePlanPort;

    @PostMapping
    public ResponseEntity<PlanResponse> create(@Valid @RequestBody PlanRequest request) {
        PlanDomain created = createPlanPort.execute(new Context(request.toDomain(null)));
        return ResponseEntity.created(URI.create("/api/plans/" + created.getId())).body(PlanResponse.from(created));
    }

    @GetMapping
    public ResponseEntity<List<PlanResponse>> findAll() {
        List<PlanResponse> plans = listPlansPort.execute(new Context()).stream().map(PlanResponse::from).toList();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(PlanResponse.from(findPlanPort.execute(new Context(id))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanResponse> update(@PathVariable UUID id, @Valid @RequestBody PlanRequest request) {
        PlanDomain updated = updatePlanPort.execute(new Context(request.toDomain(id)));
        return ResponseEntity.ok(PlanResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deletePlanPort.execute(new Context(id));
        return ResponseEntity.noContent().build();
    }
}
