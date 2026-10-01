package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.IssueRpsRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.IssueRpsResponse;
import br.gravita.core.ports.inbound.tax.IssueRpsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/nfse")
public class NfseController {

	private final IssueRpsUseCase issueRpsUseCase;

	@PostMapping("/rps")
	public ResponseEntity<IssueRpsResponse> issueRps(@Valid @RequestBody IssueRpsRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(IssueRpsResponse.from(issueRpsUseCase.execute(request.toCommand())));
	}
}
