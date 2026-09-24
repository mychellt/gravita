package br.gravita.adapters.inbound.controllers.inventory.controllers;

import br.gravita.adapters.inbound.controllers.inventory.dtos.StartPhysicalCountRequest;
import br.gravita.adapters.inbound.controllers.inventory.dtos.StartPhysicalCountResponse;
import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.ports.inbound.inventory.StartPhysicalCountUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/inventory/counts")
public class PhysicalCountController {

    private final StartPhysicalCountUseCase startPhysicalCountUseCase;

    @PostMapping
    public ResponseEntity<StartPhysicalCountResponse> start(@Valid @RequestBody StartPhysicalCountRequest request) {
        PhysicalCount physicalCount = startPhysicalCountUseCase.execute(request.toCommand());
        return ResponseEntity.created(URI.create("/api/inventory/counts/" + physicalCount.getId().value()))
                .body(StartPhysicalCountResponse.from(physicalCount));
    }
}
