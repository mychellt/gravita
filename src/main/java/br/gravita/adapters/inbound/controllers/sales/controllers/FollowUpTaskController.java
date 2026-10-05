package br.gravita.adapters.inbound.controllers.sales.controllers;

import br.gravita.adapters.inbound.controllers.sales.dtos.FollowUpTaskResponse;
import br.gravita.adapters.inbound.controllers.sales.dtos.ScheduleFollowUpTaskRequest;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.FollowUpTaskView;
import br.gravita.core.ports.inbound.sales.ScheduleFollowUpTaskUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/crm/follow-ups")
public class FollowUpTaskController {

	private final ScheduleFollowUpTaskUseCase scheduleFollowUpTaskUseCase;

	@PostMapping
	public ResponseEntity<FollowUpTaskResponse> schedule(@Valid @RequestBody final ScheduleFollowUpTaskRequest request) {
		final FollowUpTaskView created = scheduleFollowUpTaskUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/crm/follow-ups/" + created.id()))
				.body(FollowUpTaskResponse.from(created));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(final BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
