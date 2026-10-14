package br.gravita.adapters.inbound.controllers.auxiliary;

import br.gravita.adapters.dtos.request.TransportModalityRequest;
import br.gravita.adapters.dtos.response.TransportModalityResponse;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.TransportModalityDomain;
import br.gravita.core.ports.business.CreateTransportModalityPort;
import br.gravita.core.ports.business.DeleteTransportModalityPort;
import br.gravita.core.ports.business.FindTransportModalityPort;
import br.gravita.core.ports.business.ListTransportModalitiesPort;
import br.gravita.core.ports.business.UpdateTransportModalityPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Transport-Modalidade registry: the options of the NF-e form's Transporte > Modalidade select. */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auxiliary/transport-modalities")
public class TransportModalityRestController {

    private final CreateTransportModalityPort createTransportModalityPort;
    private final FindTransportModalityPort findTransportModalityPort;
    private final ListTransportModalitiesPort listTransportModalitiesPort;
    private final UpdateTransportModalityPort updateTransportModalityPort;
    private final DeleteTransportModalityPort deleteTransportModalityPort;

    @PostMapping
    public ResponseEntity<TransportModalityResponse> create(@Valid @RequestBody final TransportModalityRequest request) {
        final TransportModalityDomain created = createTransportModalityPort.execute(new Context(request.toDomain(null)));
        return ResponseEntity.created(URI.create("/api/auxiliary/transport-modalities/" + created.getId()))
                .body(TransportModalityResponse.from(created));
    }

    /** Active modalities only (what the NF-e dropdown consumes); {@code includeInactive=true} is for the admin screen. */
    @GetMapping
    public ResponseEntity<List<TransportModalityResponse>> findAll(@RequestParam(defaultValue = "false") final boolean includeInactive) {
        return ResponseEntity.ok(listTransportModalitiesPort.execute(new Context(includeInactive)).stream()
                .map(TransportModalityResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransportModalityResponse> findById(@PathVariable final UUID id) {
        return ResponseEntity.ok(TransportModalityResponse.from(findTransportModalityPort.execute(new Context(id))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TransportModalityResponse> update(@PathVariable final UUID id, @Valid @RequestBody final TransportModalityRequest request) {
        return ResponseEntity.ok(TransportModalityResponse.from(updateTransportModalityPort.execute(new Context(request.toDomain(id)))));
    }

    /** Soft delete: deactivates the modality, issued NF-e documents are not affected. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final UUID id) {
        deleteTransportModalityPort.execute(new Context(id));
        return ResponseEntity.noContent().build();
    }
}
