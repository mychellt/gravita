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
import br.gravita.core.ports.inbound.finance.AttachPayableDocumentCommand;
import br.gravita.core.ports.inbound.finance.AttachPayableDocumentUseCase;
import br.gravita.core.ports.inbound.finance.BatchPayUseCase;
import br.gravita.core.ports.inbound.finance.CreateManualPayableUseCase;
import br.gravita.core.ports.inbound.finance.PayViaPixUseCase;
import br.gravita.core.ports.inbound.finance.SplitPayableByCostCenterUseCase;
import br.gravita.core.ports.outbound.finance.DocumentAttachmentStoragePort.DocumentStorageUnavailableException;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/finance/payables")
public class PayableController {

	private final CreateManualPayableUseCase createManualPayableUseCase;
	private final SplitPayableByCostCenterUseCase splitPayableByCostCenterUseCase;
	private final ApprovePayableUseCase approvePayableUseCase;
	private final BatchPayUseCase batchPayUseCase;
	private final PayViaPixUseCase payViaPixUseCase;
	private final AttachPayableDocumentUseCase attachPayableDocumentUseCase;

	@PostMapping
	public ResponseEntity<PayableResponse> create(@Valid @RequestBody final CreateManualPayableRequest request) {
		final Payable created = createManualPayableUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/finance/payables/" + created.getId().value()))
				.body(PayableResponse.from(created));
	}

	@PatchMapping("/{id}")
	public PayableResponse split(@PathVariable final UUID id, @Valid @RequestBody final SplitPayableRequest request) {
		return PayableResponse.from(splitPayableByCostCenterUseCase.execute(request.toCommand(id)));
	}

	@PostMapping("/{id}/approve")
	public PayableResponse approve(@PathVariable final UUID id, @Valid @RequestBody final ApprovePayableRequest request) {
		return PayableResponse.from(approvePayableUseCase.execute(request.toCommand(id)));
	}

	@PostMapping("/batch-pay")
	public CnabRemittanceResponse batchPay(@Valid @RequestBody final BatchPayRequest request) {
		return CnabRemittanceResponse.from(batchPayUseCase.execute(request.toCommand()));
	}

	@PostMapping("/{id}/pix-pay")
	public PayableResponse pixPay(@PathVariable final UUID id, @Valid @RequestBody final PayViaPixRequest request) {
		return PayableResponse.from(payViaPixUseCase.execute(request.toCommand(id)));
	}

	@PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<PayableResponse> attach(@PathVariable final UUID id,
			@RequestPart("file") final MultipartFile file) {
		final Payable payable = attachPayableDocumentUseCase.execute(new AttachPayableDocumentCommand(id,
				new AttachPayableDocumentCommand.File(fileNameOf(file), contentTypeOf(file), readBytes(file))));
		return ResponseEntity.status(HttpStatus.CREATED).body(PayableResponse.from(payable));
	}

	private static String fileNameOf(final MultipartFile file) {
		final String fileName = file.getOriginalFilename();
		return fileName == null || fileName.isBlank() ? "document" : fileName;
	}

	private static String contentTypeOf(final MultipartFile file) {
		final String contentType = file.getContentType();
		return contentType == null || contentType.isBlank() ? MediaType.APPLICATION_OCTET_STREAM_VALUE
				: contentType;
	}

	private static byte[] readBytes(final MultipartFile file) {
		try {
			return file.getBytes();
		} catch (final IOException exception) {
			throw new UncheckedIOException("Unable to read uploaded document", exception);
		}
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleUserNotFoundException(final UserNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleResourceNotFoundException(final ResourceNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(final BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(BankIntegrationUnavailableException.class)
	public ResponseEntity<Map<String, String>> handleBankIntegrationUnavailableException(
			final BankIntegrationUnavailableException exception) {
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(DocumentStorageUnavailableException.class)
	public ResponseEntity<Map<String, String>> handleDocumentStorageUnavailableException(
			final DocumentStorageUnavailableException exception) {
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", exception.getMessage()));
	}
}
