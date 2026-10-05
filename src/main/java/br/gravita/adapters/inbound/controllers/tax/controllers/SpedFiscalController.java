package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.GenerateSpedFiscalRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.SpedFiscalResponse;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sped/fiscal")
public class SpedFiscalController {

	private final GenerateSpedFiscalUseCase generateSpedFiscalUseCase;

	@PostMapping
	public ResponseEntity<SpedFiscalResponse> generate(@Valid @RequestBody final GenerateSpedFiscalRequest request) {
		return ResponseEntity.ok(SpedFiscalResponse.from(generateSpedFiscalUseCase.execute(request.toCommand())));
	}
}
