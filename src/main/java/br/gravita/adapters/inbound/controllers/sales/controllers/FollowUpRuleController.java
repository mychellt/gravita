package br.gravita.adapters.inbound.controllers.sales.controllers;

import br.gravita.adapters.inbound.controllers.sales.dtos.CreateFollowUpRuleRequest;
import br.gravita.adapters.inbound.controllers.sales.dtos.FollowUpRuleResponse;
import br.gravita.adapters.inbound.controllers.sales.dtos.UpdateFollowUpRuleRequest;
import br.gravita.core.domain.sales.FollowUpRuleId;
import br.gravita.core.domain.sales.FollowUpRuleNotFoundException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.ListFollowUpRulesUseCase;
import br.gravita.core.ports.inbound.sales.ManageFollowUpRuleUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/crm/follow-up-rules")
public class FollowUpRuleController {

	private final ManageFollowUpRuleUseCase manageFollowUpRuleUseCase;
	private final ListFollowUpRulesUseCase listFollowUpRulesUseCase;

	@PostMapping
	public ResponseEntity<FollowUpRuleResponse> create(@Valid @RequestBody CreateFollowUpRuleRequest request) {
		FollowUpRuleResponse created =
				FollowUpRuleResponse.from(manageFollowUpRuleUseCase.create(request.toCommand()));
		return ResponseEntity.created(URI.create("/api/crm/follow-up-rules/" + created.id())).body(created);
	}

	@GetMapping
	public ResponseEntity<List<FollowUpRuleResponse>> list() {
		List<FollowUpRuleResponse> response =
				listFollowUpRulesUseCase.execute().stream().map(FollowUpRuleResponse::from).toList();
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{id}")
	public ResponseEntity<FollowUpRuleResponse> update(@PathVariable UUID id,
			@Valid @RequestBody UpdateFollowUpRuleRequest request) {
		FollowUpRuleResponse updated =
				FollowUpRuleResponse.from(manageFollowUpRuleUseCase.update(request.toCommand(id)));
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		manageFollowUpRuleUseCase.delete(FollowUpRuleId.of(id));
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(FollowUpRuleNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleFollowUpRuleNotFoundException(
			FollowUpRuleNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
