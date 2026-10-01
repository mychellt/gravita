package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.ConvertRpsToNfseRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.ConvertRpsToNfseResponse;
import br.gravita.adapters.inbound.controllers.tax.dtos.IssueRpsRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.IssueRpsResponse;
import br.gravita.adapters.inbound.controllers.tax.dtos.TransmitNfseResponse;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.ports.inbound.tax.ConvertRpsToNfseUseCase;
import br.gravita.core.ports.inbound.tax.IssueRpsUseCase;
import br.gravita.core.ports.inbound.tax.TransmitNfseCommand;
import br.gravita.core.ports.inbound.tax.TransmitNfseUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/nfse")
public class NfseController {

	private final IssueRpsUseCase issueRpsUseCase;
	private final ConvertRpsToNfseUseCase convertRpsToNfseUseCase;
	private final TransmitNfseUseCase transmitNfseUseCase;

	@PostMapping("/rps")
	public ResponseEntity<IssueRpsResponse> issueRps(@Valid @RequestBody IssueRpsRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(IssueRpsResponse.from(issueRpsUseCase.execute(request.toCommand())));
	}

	@PostMapping("/rps/convert")
	public ResponseEntity<ConvertRpsToNfseResponse> convertRps(@Valid @RequestBody ConvertRpsToNfseRequest request) {
		return ResponseEntity.ok(ConvertRpsToNfseResponse.from(convertRpsToNfseUseCase.execute(request.toCommand())));
	}

	/** Follow-up action on a {@code DRAFT} NFSe (the convert endpoint does not transmit). */
	@PostMapping("/{id}/transmit")
	public ResponseEntity<TransmitNfseResponse> transmit(@PathVariable UUID id) {
		return ResponseEntity.ok(TransmitNfseResponse
				.from(transmitNfseUseCase.execute(new TransmitNfseCommand(NfseId.of(id)))));
	}
}
