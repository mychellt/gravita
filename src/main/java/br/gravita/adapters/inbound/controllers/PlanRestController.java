package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.PlanRequest;
import br.gravita.adapters.dtos.response.PlanResponse;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.ports.business.CreatePlanPort;
import br.gravita.core.ports.business.DeletePlanPort;
import br.gravita.core.ports.business.FindPlanPort;
import br.gravita.core.ports.business.ListPlansPort;
import br.gravita.core.ports.business.UpdatePlanPort;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/plans")
public class PlanRestController {

	private final CreatePlanPort createPlanPort;
	private final FindPlanPort findPlanPort;
	private final ListPlansPort listPlansPort;
	private final UpdatePlanPort updatePlanPort;
	private final DeletePlanPort deletePlanPort;

	public PlanRestController(CreatePlanPort createPlanPort, FindPlanPort findPlanPort, ListPlansPort listPlansPort,
			UpdatePlanPort updatePlanPort, DeletePlanPort deletePlanPort) {
		this.createPlanPort = createPlanPort;
		this.findPlanPort = findPlanPort;
		this.listPlansPort = listPlansPort;
		this.updatePlanPort = updatePlanPort;
		this.deletePlanPort = deletePlanPort;
	}

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
