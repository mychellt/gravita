package br.gravita.adapters.inbound.controllers.sales.controllers;

import br.gravita.adapters.inbound.controllers.sales.dtos.ChangeOpportunityStageRequest;
import br.gravita.adapters.inbound.controllers.sales.dtos.CreateOpportunityRequest;
import br.gravita.adapters.inbound.controllers.sales.dtos.OpportunityResponse;
import br.gravita.adapters.inbound.controllers.sales.dtos.UpdateOpportunityRequest;
import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.domain.sales.OpportunityNotFoundException;
import br.gravita.core.domain.sales.OpportunityStage;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.GetOpportunityQuery;
import br.gravita.core.ports.inbound.sales.GetOpportunityUseCase;
import br.gravita.core.ports.inbound.sales.ListOpportunitiesQuery;
import br.gravita.core.ports.inbound.sales.ListOpportunitiesUseCase;
import br.gravita.core.ports.inbound.sales.ManageOpportunityUseCase;
import br.gravita.core.ports.inbound.sales.OpportunityView;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/crm/opportunities")
public class OpportunityController {

	private final ManageOpportunityUseCase manageOpportunityUseCase;
	private final GetOpportunityUseCase getOpportunityUseCase;
	private final ListOpportunitiesUseCase listOpportunitiesUseCase;

	@PostMapping
	public ResponseEntity<OpportunityResponse> create(@Valid @RequestBody CreateOpportunityRequest request) {
		OpportunityView created = manageOpportunityUseCase.create(request.toCommand());
		return ResponseEntity.created(URI.create("/api/crm/opportunities/" + created.id()))
				.body(OpportunityResponse.from(created));
	}

	@GetMapping
	public ResponseEntity<List<OpportunityResponse>> list(@RequestParam(required = false) OpportunityStage stage) {
		List<OpportunityResponse> response = listOpportunitiesUseCase.execute(new ListOpportunitiesQuery(stage))
				.stream()
				.map(OpportunityResponse::from)
				.toList();
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<OpportunityResponse> findById(@PathVariable UUID id) {
		OpportunityView view = getOpportunityUseCase.execute(new GetOpportunityQuery(OpportunityId.of(id)));
		return ResponseEntity.ok(OpportunityResponse.from(view));
	}

	@PatchMapping("/{id}")
	public ResponseEntity<OpportunityResponse> update(@PathVariable UUID id,
			@Valid @RequestBody UpdateOpportunityRequest request) {
		OpportunityView updated = manageOpportunityUseCase.update(request.toCommand(id));
		return ResponseEntity.ok(OpportunityResponse.from(updated));
	}

	@PatchMapping("/{id}/stage")
	public ResponseEntity<OpportunityResponse> changeStage(@PathVariable UUID id,
			@Valid @RequestBody ChangeOpportunityStageRequest request) {
		OpportunityView updated = manageOpportunityUseCase.changeStage(request.toCommand(id));
		return ResponseEntity.ok(OpportunityResponse.from(updated));
	}

	@ExceptionHandler(OpportunityNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleOpportunityNotFoundException(
			OpportunityNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
