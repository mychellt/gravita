package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.IssueNfeRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.IssueNfeResponse;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.ports.inbound.tax.IssueNfeUseCase;
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
@RequestMapping("/api/nfe")
public class NfeController {

	private final IssueNfeUseCase issueNfeUseCase;

	@PostMapping
	public ResponseEntity<IssueNfeResponse> issue(@Valid @RequestBody IssueNfeRequest request) {
		NfeDocument document = issueNfeUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/nfe/" + document.getId().value()))
				.body(IssueNfeResponse.from(document));
	}
}
