package br.gravita.adapters.inbound.controllers.finance.controllers;

import br.gravita.adapters.inbound.controllers.finance.dtos.BoletoResponse;
import br.gravita.adapters.inbound.controllers.finance.dtos.CreateManualReceivableRequest;
import br.gravita.adapters.inbound.controllers.finance.dtos.GenerateBoletoRequest;
import br.gravita.adapters.inbound.controllers.finance.dtos.PixChargeResponse;
import br.gravita.adapters.inbound.controllers.finance.dtos.ReceivableResponse;
import br.gravita.adapters.inbound.controllers.finance.dtos.RenegotiateTitleRequest;
import br.gravita.adapters.inbound.controllers.finance.dtos.RenegotiationResponse;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.BankIntegrationUnavailableException;
import br.gravita.core.domain.finance.Boleto;
import br.gravita.core.domain.finance.PixCharge;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.Renegotiation;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.CreateManualReceivableUseCase;
import br.gravita.core.ports.inbound.finance.GenerateBoletoUseCase;
import br.gravita.core.ports.inbound.finance.GeneratePixChargeCommand;
import br.gravita.core.ports.inbound.finance.GeneratePixChargeUseCase;
import br.gravita.core.ports.inbound.finance.RenegotiateTitleUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/finance/receivables")
public class ReceivableController {

	private final CreateManualReceivableUseCase createManualReceivableUseCase;
	private final GenerateBoletoUseCase generateBoletoUseCase;
	private final GeneratePixChargeUseCase generatePixChargeUseCase;
	private final RenegotiateTitleUseCase renegotiateTitleUseCase;

	@PostMapping
	public ResponseEntity<ReceivableResponse> create(@Valid @RequestBody CreateManualReceivableRequest request) {
		Receivable created = createManualReceivableUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/finance/receivables/" + created.getId().value()))
				.body(ReceivableResponse.from(created));
	}

	@PostMapping("/{id}/boleto")
	public ResponseEntity<BoletoResponse> generateBoleto(@PathVariable UUID id,
			@Valid @RequestBody GenerateBoletoRequest request) {
		Boleto boleto = generateBoletoUseCase.execute(request.toCommand(id));
		return ResponseEntity.status(HttpStatus.CREATED).body(BoletoResponse.from(boleto));
	}

	@PostMapping("/{id}/pix-charge")
	public ResponseEntity<PixChargeResponse> generatePixCharge(@PathVariable UUID id) {
		PixCharge pixCharge = generatePixChargeUseCase.execute(new GeneratePixChargeCommand(id));
		return ResponseEntity.status(HttpStatus.CREATED).body(PixChargeResponse.from(pixCharge));
	}

	@PostMapping("/{id}/renegotiate")
	public ResponseEntity<RenegotiationResponse> renegotiate(@PathVariable UUID id,
			@Valid @RequestBody RenegotiateTitleRequest request) {
		Renegotiation renegotiation = renegotiateTitleUseCase.execute(request.toCommand(id));
		return ResponseEntity.status(HttpStatus.CREATED).body(RenegotiationResponse.from(renegotiation));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(BankIntegrationUnavailableException.class)
	public ResponseEntity<Map<String, String>> handleBankIntegrationUnavailableException(
			BankIntegrationUnavailableException exception) {
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", exception.getMessage()));
	}
}
