package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.CancelNfceRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.RegisterNfceSaleRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.RegisterNfceSaleResponse;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.ports.inbound.tax.CancelNfceUseCase;
import br.gravita.core.ports.inbound.tax.IssueNfceCommand;
import br.gravita.core.ports.inbound.tax.IssueNfceUseCase;
import br.gravita.core.ports.inbound.tax.RegisterNfceSaleUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/pdv/sales")
public class NfceSaleController {

	private final RegisterNfceSaleUseCase registerNfceSaleUseCase;
	private final IssueNfceUseCase issueNfceUseCase;
	private final CancelNfceUseCase cancelNfceUseCase;

	@PostMapping
	public ResponseEntity<RegisterNfceSaleResponse> register(@Valid @RequestBody final RegisterNfceSaleRequest request) {
		final NfceSaleId id = registerNfceSaleUseCase.execute(request.toCommand());
		// The sale is already stored; a failure issuing its NFC-e must not hide that, so the sale stays DRAFT and says why.
		RegisterNfceSaleResponse body;
		try {
			body = RegisterNfceSaleResponse.from(id, issueNfceUseCase.execute(new IssueNfceCommand(id.value())));
		} catch (final RuntimeException issuanceFailure) {
			log.warn("NFC-e issuance failed for sale {}; it stays DRAFT", id.value(), issuanceFailure);
			body = RegisterNfceSaleResponse.notIssued(id, issuanceFailure.getMessage());
		}
		return ResponseEntity.created(URI.create("/api/pdv/sales/" + id.value())).body(body);
	}

	@PostMapping("/{id}/cancel")
	public ResponseEntity<Void> cancel(@PathVariable final UUID id, @Valid @RequestBody final CancelNfceRequest request) {
		cancelNfceUseCase.execute(request.toCommand(id));
		return ResponseEntity.noContent().build();
	}
}
