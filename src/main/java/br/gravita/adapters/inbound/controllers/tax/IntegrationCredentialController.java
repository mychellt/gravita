package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.usercases.system.ConfigureIntegrationCredentialCommand;
import br.gravita.core.usercases.system.ConfigureIntegrationCredentialUseCase;
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
@RequestMapping("/api/system/integrations")
public class IntegrationCredentialController {

	private final ConfigureIntegrationCredentialUseCase configureIntegrationCredentialUseCase;

	public IntegrationCredentialController(final ConfigureIntegrationCredentialUseCase configureIntegrationCredentialUseCase) {
		this.configureIntegrationCredentialUseCase = configureIntegrationCredentialUseCase;
	}

	@PutMapping("/{name}/credentials")
	public ResponseEntity<Void> configureCredentials(@PathVariable("name") final String name,
			@Valid @RequestBody final ConfigureIntegrationCredentialRequest request) {
		configureIntegrationCredentialUseCase.execute(new ConfigureIntegrationCredentialCommand(name,
				request.environment(), request.endpoint(), request.credentialPayload()));
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(final BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}
}
