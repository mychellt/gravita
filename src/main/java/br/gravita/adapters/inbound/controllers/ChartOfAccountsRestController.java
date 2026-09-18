package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.ChartOfAccountsRequest;
import br.gravita.adapters.dtos.response.ChartOfAccountsResponse;
import br.gravita.core.domain.ChartOfAccountsDomain;
import br.gravita.core.domain.Context;
import br.gravita.core.ports.business.CreateChartOfAccountsPort;
import br.gravita.core.ports.business.DeleteChartOfAccountsPort;
import br.gravita.core.ports.business.FindChartOfAccountsPort;
import br.gravita.core.ports.business.ListChartOfAccountsPort;
import br.gravita.core.ports.business.UpdateChartOfAccountsPort;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Returns a flat list; the tree is reconstructed client-side via {@code parentId} (AC: hierarchical tree, not a flat list). */
@RestController
@RequestMapping("/api/auxiliary/chart-of-accounts")
public class ChartOfAccountsRestController {

	private final CreateChartOfAccountsPort createChartOfAccountsPort;
	private final FindChartOfAccountsPort findChartOfAccountsPort;
	private final ListChartOfAccountsPort listChartOfAccountsPort;
	private final UpdateChartOfAccountsPort updateChartOfAccountsPort;
	private final DeleteChartOfAccountsPort deleteChartOfAccountsPort;

	public ChartOfAccountsRestController(CreateChartOfAccountsPort createChartOfAccountsPort, FindChartOfAccountsPort findChartOfAccountsPort,
			ListChartOfAccountsPort listChartOfAccountsPort, UpdateChartOfAccountsPort updateChartOfAccountsPort,
			DeleteChartOfAccountsPort deleteChartOfAccountsPort) {
		this.createChartOfAccountsPort = createChartOfAccountsPort;
		this.findChartOfAccountsPort = findChartOfAccountsPort;
		this.listChartOfAccountsPort = listChartOfAccountsPort;
		this.updateChartOfAccountsPort = updateChartOfAccountsPort;
		this.deleteChartOfAccountsPort = deleteChartOfAccountsPort;
	}

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
