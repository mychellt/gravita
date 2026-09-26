package br.gravita.adapters.inbound.controllers.auxiliary;

import br.gravita.adapters.dtos.request.ChartOfAccountsRequest;
import br.gravita.adapters.dtos.response.ChartOfAccountsResponse;
import br.gravita.core.domain.ChartOfAccountsDomain;
import br.gravita.core.domain.Context;
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
@RequestMapping("/api/auxiliary/chart-of-accounts")
public class ChartOfAccountsRestController {

    private final CreateChartOfAccountsPort createChartOfAccountsPort;
    private final FindChartOfAccountsPort findChartOfAccountsPort;
    private final ListChartOfAccountsPort listChartOfAccountsPort;
    private final UpdateChartOfAccountsPort updateChartOfAccountsPort;
    private final DeleteChartOfAccountsPort deleteChartOfAccountsPort;

    @PostMapping
    public ResponseEntity<ChartOfAccountsResponse> create(@Valid @RequestBody ChartOfAccountsRequest request) {
        ChartOfAccountsDomain created = createChartOfAccountsPort.execute(new Context(request.toDomain(null)));
        return ResponseEntity.created(URI.create("/api/auxiliary/chart-of-accounts/" + created.getId()))
                .body(ChartOfAccountsResponse.from(created));
    }

    @GetMapping
    public ResponseEntity<List<ChartOfAccountsResponse>> findAll() {
        return ResponseEntity.ok(listChartOfAccountsPort.execute(new Context()).stream().map(ChartOfAccountsResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChartOfAccountsResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ChartOfAccountsResponse.from(findChartOfAccountsPort.execute(new Context(id))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ChartOfAccountsResponse> update(@PathVariable UUID id, @Valid @RequestBody ChartOfAccountsRequest request) {
        return ResponseEntity.ok(ChartOfAccountsResponse.from(updateChartOfAccountsPort.execute(new Context(request.toDomain(id)))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteChartOfAccountsPort.execute(new Context(id));
        return ResponseEntity.noContent().build();
    }
}
