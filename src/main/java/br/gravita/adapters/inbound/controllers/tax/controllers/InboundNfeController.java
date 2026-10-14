package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.ConfirmInboundNfeReceiptRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.EnterInboundNfeManuallyRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.ImportSupplierNfeXmlResponse;
import br.gravita.adapters.inbound.controllers.security.AuthenticatedUser;
import br.gravita.adapters.inbound.controllers.tax.dtos.InboundManifestationResponse;
import br.gravita.adapters.inbound.controllers.tax.dtos.InboundNfeListItemResponse;
import br.gravita.adapters.inbound.controllers.tax.dtos.ManifestInboundNfeRequest;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.tax.InboundNfeNotFoundException;
import br.gravita.core.ports.inbound.tax.ConfirmInboundNfeReceiptUseCase;
import br.gravita.core.ports.inbound.tax.EnterInboundNfeManuallyUseCase;
import br.gravita.core.ports.inbound.tax.ImportSupplierNfeXmlCommand;
import br.gravita.core.ports.inbound.tax.ImportSupplierNfeXmlUseCase;
import br.gravita.core.ports.inbound.tax.ListInboundNfeUseCase;
import br.gravita.core.ports.inbound.tax.ManifestInboundNfeUseCase;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/nfe/inbound")
public class InboundNfeController {

	private final ImportSupplierNfeXmlUseCase importSupplierNfeXmlUseCase;
	private final EnterInboundNfeManuallyUseCase enterInboundNfeManuallyUseCase;
	private final ConfirmInboundNfeReceiptUseCase confirmInboundNfeReceiptUseCase;
	private final ManifestInboundNfeUseCase manifestInboundNfeUseCase;
	private final ListInboundNfeUseCase listInboundNfeUseCase;

	public InboundNfeController(final ImportSupplierNfeXmlUseCase importSupplierNfeXmlUseCase,
			final EnterInboundNfeManuallyUseCase enterInboundNfeManuallyUseCase,
			final ConfirmInboundNfeReceiptUseCase confirmInboundNfeReceiptUseCase,
			final ManifestInboundNfeUseCase manifestInboundNfeUseCase,
			final ListInboundNfeUseCase listInboundNfeUseCase) {
		this.importSupplierNfeXmlUseCase = importSupplierNfeXmlUseCase;
		this.enterInboundNfeManuallyUseCase = enterInboundNfeManuallyUseCase;
		this.confirmInboundNfeReceiptUseCase = confirmInboundNfeReceiptUseCase;
		this.manifestInboundNfeUseCase = manifestInboundNfeUseCase;
		this.listInboundNfeUseCase = listInboundNfeUseCase;
	}

	/** All inbound NF-e of the caller's company, any status, newest first. */
	@GetMapping
	public List<InboundNfeListItemResponse> findAll(@AuthenticatedUser final UserId callerId) {
		return listInboundNfeUseCase.execute(new Context().withCaller(callerId)).stream()
				.map(InboundNfeListItemResponse::from).toList();
	}

	@PostMapping(value = "/import-xml", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ImportSupplierNfeXmlResponse> importXml(@RequestParam final UUID companyId,
			@RequestPart("xmlFile") final MultipartFile xmlFile) {
		final var inboundNfe = importSupplierNfeXmlUseCase
				.execute(new ImportSupplierNfeXmlCommand(CompanyId.of(companyId), readBytes(xmlFile)));
		return ResponseEntity.status(HttpStatus.CREATED).body(ImportSupplierNfeXmlResponse.from(inboundNfe));
	}

	@PostMapping
	public ResponseEntity<ImportSupplierNfeXmlResponse> enterManually(
			@RequestBody final EnterInboundNfeManuallyRequest request) {
		final var inboundNfe = enterInboundNfeManuallyUseCase.execute(request.toCommand());
		return ResponseEntity.status(HttpStatus.CREATED).body(ImportSupplierNfeXmlResponse.from(inboundNfe));
	}

	@PostMapping("/{id}/confirm-receipt")
	public ResponseEntity<ImportSupplierNfeXmlResponse> confirmReceipt(@PathVariable final UUID id,
			@RequestBody final ConfirmInboundNfeReceiptRequest request) {
		final var inboundNfe = confirmInboundNfeReceiptUseCase.execute(request.toCommand(id));
		return ResponseEntity.ok(ImportSupplierNfeXmlResponse.from(inboundNfe));
	}

	@PostMapping("/manifestation")
	public ResponseEntity<InboundManifestationResponse> manifest(@RequestBody final ManifestInboundNfeRequest request) {
		final var manifestation = manifestInboundNfeUseCase.execute(request.toCommand());
		return ResponseEntity.status(HttpStatus.CREATED).body(InboundManifestationResponse.from(manifestation));
	}

	private byte[] readBytes(final MultipartFile file) {
		try {
			return file.getBytes();
		} catch (final IOException e) {
			throw new UncheckedIOException("Unable to read uploaded NFe XML file", e);
		}
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(final BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(InboundNfeNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleInboundNfeNotFoundException(
			final InboundNfeNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}
}
