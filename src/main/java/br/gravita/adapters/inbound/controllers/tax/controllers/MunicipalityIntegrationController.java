package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.RegisterMunicipalityIntegrationRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.RegisterMunicipalityIntegrationResponse;
import br.gravita.core.ports.inbound.tax.RegisterMunicipalityIntegrationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/municipalities")
public class MunicipalityIntegrationController {

	private final RegisterMunicipalityIntegrationUseCase registerMunicipalityIntegrationUseCase;

	@PostMapping("/{ibgeCode}/integration")
	public ResponseEntity<RegisterMunicipalityIntegrationResponse> register(@PathVariable final String ibgeCode,
			@Valid @RequestBody final RegisterMunicipalityIntegrationRequest request) {
		return ResponseEntity.ok(RegisterMunicipalityIntegrationResponse
				.from(registerMunicipalityIntegrationUseCase.execute(request.toCommand(ibgeCode))));
	}
}
