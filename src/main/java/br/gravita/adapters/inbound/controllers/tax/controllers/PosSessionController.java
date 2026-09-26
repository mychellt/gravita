package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.OpenPosSessionRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.OpenPosSessionResponse;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.ports.inbound.tax.OpenPosSessionUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/pdv/sessions")
public class PosSessionController {

	private final OpenPosSessionUseCase openPosSessionUseCase;

	@PostMapping
	public ResponseEntity<OpenPosSessionResponse> open(@Valid @RequestBody OpenPosSessionRequest request) {
		PosSessionId id = openPosSessionUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/pdv/sessions/" + id.value()))
				.body(OpenPosSessionResponse.from(id));
	}
}
