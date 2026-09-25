package br.gravita.adapters.inbound.controllers.inventory.controllers;

import br.gravita.adapters.inbound.controllers.inventory.dtos.ApprovePhysicalCountRequest;
import br.gravita.adapters.inbound.controllers.inventory.dtos.ApprovePhysicalCountResponse;
import br.gravita.adapters.inbound.controllers.inventory.dtos.StartPhysicalCountRequest;
import br.gravita.adapters.inbound.controllers.inventory.dtos.StartPhysicalCountResponse;
import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.ports.inbound.inventory.ApprovePhysicalCountUseCase;
import br.gravita.core.ports.inbound.inventory.StartPhysicalCountUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/inventory/counts")
public class PhysicalCountController {

    private final StartPhysicalCountUseCase startPhysicalCountUseCase;
    private final ApprovePhysicalCountUseCase approvePhysicalCountUseCase;

    @PostMapping
    public ResponseEntity<StartPhysicalCountResponse> start(@Valid @RequestBody StartPhysicalCountRequest request) {
        PhysicalCount physicalCount = startPhysicalCountUseCase.execute(request.toCommand());
        return ResponseEntity.created(URI.create("/api/inventory/counts/" + physicalCount.getId().value()))
                .body(StartPhysicalCountResponse.from(physicalCount));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApprovePhysicalCountResponse> approve(@PathVariable UUID id,
            @Valid @RequestBody ApprovePhysicalCountRequest request) {
        PhysicalCount physicalCount = approvePhysicalCountUseCase.execute(request.toCommand(id));
        return ResponseEntity.ok(ApprovePhysicalCountResponse.from(physicalCount));
    }
}
