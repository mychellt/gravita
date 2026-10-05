package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.usercases.system.ConfigureApprovalAlcadaUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system/alcadas")
public class ApprovalAlcadaController {

	private final ConfigureApprovalAlcadaUseCase configureApprovalAlcadaUseCase;

	public ApprovalAlcadaController(final ConfigureApprovalAlcadaUseCase configureApprovalAlcadaUseCase) {
		this.configureApprovalAlcadaUseCase = configureApprovalAlcadaUseCase;
	}

	@PutMapping("/{module}")
	public ResponseEntity<Void> configure(@PathVariable("module") final String module,
			@Valid @RequestBody final ConfigureApprovalAlcadaRequest request) {
		configureApprovalAlcadaUseCase.execute(request.toCommand(module));
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(final BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
