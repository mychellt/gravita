package br.gravita.adapters.inbound.controllers.finance.controllers;

import br.gravita.adapters.inbound.controllers.finance.dtos.ApprovePayableRequest;
import br.gravita.adapters.inbound.controllers.finance.dtos.BatchPayRequest;
import br.gravita.adapters.inbound.controllers.finance.dtos.CnabRemittanceResponse;
import br.gravita.adapters.inbound.controllers.finance.dtos.CreateManualPayableRequest;
import br.gravita.adapters.inbound.controllers.finance.dtos.PayViaPixRequest;
import br.gravita.adapters.inbound.controllers.finance.dtos.PayableResponse;
import br.gravita.adapters.inbound.controllers.finance.dtos.SplitPayableRequest;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.BankIntegrationUnavailableException;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.UserNotFoundException;
import br.gravita.core.ports.inbound.finance.ApprovePayableUseCase;
import br.gravita.core.ports.inbound.finance.BatchPayUseCase;
import br.gravita.core.ports.inbound.finance.CreateManualPayableUseCase;
import br.gravita.core.ports.inbound.finance.PayViaPixUseCase;
import br.gravita.core.ports.inbound.finance.SplitPayableByCostCenterUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/finance/payables")
public class PayableController {

	private final CreateManualPayableUseCase createManualPayableUseCase;
	private final SplitPayableByCostCenterUseCase splitPayableByCostCenterUseCase;
	private final ApprovePayableUseCase approvePayableUseCase;
	private final BatchPayUseCase batchPayUseCase;
	private final PayViaPixUseCase payViaPixUseCase;

	@PostMapping
	public ResponseEntity<PayableResponse> create(@Valid @RequestBody CreateManualPayableRequest request) {
		Payable created = createManualPayableUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/finance/payables/" + created.getId().value()))
				.body(PayableResponse.from(created));
	}

	@PatchMapping("/{id}")
	public PayableResponse split(@PathVariable UUID id, @Valid @RequestBody SplitPayableRequest request) {
		return PayableResponse.from(splitPayableByCostCenterUseCase.execute(request.toCommand(id)));
	}

	@PostMapping("/{id}/approve")
	public PayableResponse approve(@PathVariable UUID id, @Valid @RequestBody ApprovePayableRequest request) {
		return PayableResponse.from(approvePayableUseCase.execute(request.toCommand(id)));
	}

	@PostMapping("/batch-pay")
	public CnabRemittanceResponse batchPay(@Valid @RequestBody BatchPayRequest request) {
		return CnabRemittanceResponse.from(batchPayUseCase.execute(request.toCommand()));
	}

	@PostMapping("/{id}/pix-pay")
	public PayableResponse pixPay(@PathVariable UUID id, @Valid @RequestBody PayViaPixRequest request) {
		return PayableResponse.from(payViaPixUseCase.execute(request.toCommand(id)));
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleUserNotFoundException(UserNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
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
