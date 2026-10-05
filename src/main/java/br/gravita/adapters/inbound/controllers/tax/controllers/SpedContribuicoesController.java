package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.GenerateSpedContribuicoesRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.SpedContribuicoesResponse;
import br.gravita.core.ports.inbound.tax.GenerateSpedContribuicoesUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sped/contribuicoes")
public class SpedContribuicoesController {

	private final GenerateSpedContribuicoesUseCase generateSpedContribuicoesUseCase;

	@PostMapping
	public ResponseEntity<SpedContribuicoesResponse> generate(
			@Valid @RequestBody final GenerateSpedContribuicoesRequest request) {
		return ResponseEntity
				.ok(SpedContribuicoesResponse.from(generateSpedContribuicoesUseCase.execute(request.toCommand())));
	}
}
