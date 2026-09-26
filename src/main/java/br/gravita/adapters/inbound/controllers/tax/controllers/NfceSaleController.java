package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.RegisterNfceSaleRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.RegisterNfceSaleResponse;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.ports.inbound.tax.IssueNfceCommand;
import br.gravita.core.ports.inbound.tax.IssueNfceUseCase;
import br.gravita.core.ports.inbound.tax.NfceIssuanceResult;
import br.gravita.core.ports.inbound.tax.RegisterNfceSaleUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Per the module spec's adapter table, {@code POST /api/pdv/sales} orchestrates
 * both UC-M3-03 (register) and UC-M3-04 (issue, here) behind one call: the
 * cashier never sees the intermediate DRAFT state.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/pdv/sales")
public class NfceSaleController {

	private final RegisterNfceSaleUseCase registerNfceSaleUseCase;
	private final IssueNfceUseCase issueNfceUseCase;

	@PostMapping
	public ResponseEntity<RegisterNfceSaleResponse> register(@Valid @RequestBody RegisterNfceSaleRequest request) {
		NfceSaleId id = registerNfceSaleUseCase.execute(request.toCommand());
		NfceIssuanceResult issuance = issueNfceUseCase.execute(new IssueNfceCommand(id.value()));
		return ResponseEntity.created(URI.create("/api/pdv/sales/" + id.value()))
				.body(RegisterNfceSaleResponse.from(id, issuance));
	}
}
