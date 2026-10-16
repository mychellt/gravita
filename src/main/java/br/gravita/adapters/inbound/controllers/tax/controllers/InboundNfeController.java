package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.dtos.request.RegisterProductRequest;
import br.gravita.adapters.dtos.response.ProductResponse;
import br.gravita.adapters.inbound.controllers.tax.dtos.ConfirmInboundNfeReceiptRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.EnterInboundNfeManuallyRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.ImportSupplierNfeXmlResponse;
import br.gravita.adapters.inbound.controllers.security.AuthenticatedUser;
import br.gravita.adapters.inbound.controllers.tax.dtos.InboundManifestationResponse;
import br.gravita.adapters.inbound.controllers.tax.dtos.InboundNfeConferenceViewResponse;
import br.gravita.adapters.inbound.controllers.tax.dtos.InboundNfeListItemResponse;
import br.gravita.adapters.inbound.controllers.tax.dtos.LinkExistingProductRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.ManifestInboundNfeRequest;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeNotFoundException;
import br.gravita.core.ports.inbound.tax.ConfirmInboundNfeReceiptUseCase;
import br.gravita.core.ports.inbound.tax.EnterInboundNfeManuallyUseCase;
import br.gravita.core.ports.inbound.tax.GetInboundNfeForConferenceUseCase;
import br.gravita.core.ports.inbound.tax.ImportSupplierNfeXmlCommand;
import br.gravita.core.ports.inbound.tax.ImportSupplierNfeXmlUseCase;
import br.gravita.core.ports.inbound.tax.ListInboundNfeUseCase;
import br.gravita.core.ports.inbound.tax.ManifestInboundNfeUseCase;
import br.gravita.core.ports.inbound.tax.ResolveInboundNfeItemAsExistingProductCommand;
import br.gravita.core.ports.inbound.tax.ResolveInboundNfeItemAsExistingProductUseCase;
import br.gravita.core.ports.inbound.tax.ResolveInboundNfeItemAsNewProductCommand;
import br.gravita.core.ports.inbound.tax.ResolveInboundNfeItemAsNewProductUseCase;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import jakarta.validation.Valid;
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
	private final GetInboundNfeForConferenceUseCase getInboundNfeForConferenceUseCase;
	private final ResolveInboundNfeItemAsNewProductUseCase resolveInboundNfeItemAsNewProductUseCase;
	private final ResolveInboundNfeItemAsExistingProductUseCase resolveInboundNfeItemAsExistingProductUseCase;
	private final SupplierRepositoryPort supplierRepositoryPort;

	public InboundNfeController(final ImportSupplierNfeXmlUseCase importSupplierNfeXmlUseCase,
			final EnterInboundNfeManuallyUseCase enterInboundNfeManuallyUseCase,
			final ConfirmInboundNfeReceiptUseCase confirmInboundNfeReceiptUseCase,
			final ManifestInboundNfeUseCase manifestInboundNfeUseCase,
			final ListInboundNfeUseCase listInboundNfeUseCase,
			final GetInboundNfeForConferenceUseCase getInboundNfeForConferenceUseCase,
			final ResolveInboundNfeItemAsNewProductUseCase resolveInboundNfeItemAsNewProductUseCase,
			final ResolveInboundNfeItemAsExistingProductUseCase resolveInboundNfeItemAsExistingProductUseCase,
			final SupplierRepositoryPort supplierRepositoryPort) {
		this.importSupplierNfeXmlUseCase = importSupplierNfeXmlUseCase;
		this.enterInboundNfeManuallyUseCase = enterInboundNfeManuallyUseCase;
		this.confirmInboundNfeReceiptUseCase = confirmInboundNfeReceiptUseCase;
		this.manifestInboundNfeUseCase = manifestInboundNfeUseCase;
		this.listInboundNfeUseCase = listInboundNfeUseCase;
		this.getInboundNfeForConferenceUseCase = getInboundNfeForConferenceUseCase;
		this.resolveInboundNfeItemAsNewProductUseCase = resolveInboundNfeItemAsNewProductUseCase;
		this.resolveInboundNfeItemAsExistingProductUseCase = resolveInboundNfeItemAsExistingProductUseCase;
		this.supplierRepositoryPort = supplierRepositoryPort;
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
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ImportSupplierNfeXmlResponse.from(inboundNfe, isSupplierResolved(inboundNfe)));
	}

	@PostMapping
	public ResponseEntity<ImportSupplierNfeXmlResponse> enterManually(
			@RequestBody final EnterInboundNfeManuallyRequest request) {
		final var inboundNfe = enterInboundNfeManuallyUseCase.execute(request.toCommand());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ImportSupplierNfeXmlResponse.from(inboundNfe, isSupplierResolved(inboundNfe)));
	}

	/** GRA-246 BR1/BR3: resolves supplier and per-item catalog links dynamically, for the conference screen to pre-fill. */
	@GetMapping("/{id}/conference")
	public ResponseEntity<InboundNfeConferenceViewResponse> getConferenceView(@PathVariable final UUID id) {
		return ResponseEntity.ok(InboundNfeConferenceViewResponse.from(getInboundNfeForConferenceUseCase.execute(id)));
	}

	/** GRA-246 BR4: registers a brand-new catalog product from this item's data and links it for next time. */
	@PostMapping("/{id}/items/{itemIndex}/link-new-product")
	public ResponseEntity<ProductResponse> linkNewProduct(@PathVariable final UUID id, @PathVariable final int itemIndex,
			@Valid @RequestBody final RegisterProductRequest request) {
		final ProductDomain created = resolveInboundNfeItemAsNewProductUseCase
				.execute(new ResolveInboundNfeItemAsNewProductCommand(id, itemIndex, request.toDomain(null)));
		return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(created));
	}

	/** GRA-246 BR5: links this item to an existing catalog product, without creating a new one. */
	@PostMapping("/{id}/items/{itemIndex}/link-existing-product")
	public ResponseEntity<Void> linkExistingProduct(@PathVariable final UUID id, @PathVariable final int itemIndex,
			@Valid @RequestBody final LinkExistingProductRequest request) {
		resolveInboundNfeItemAsExistingProductUseCase
				.execute(new ResolveInboundNfeItemAsExistingProductCommand(id, itemIndex, request.productId()));
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@PostMapping("/{id}/confirm-receipt")
	public ResponseEntity<ImportSupplierNfeXmlResponse> confirmReceipt(@PathVariable final UUID id,
			@RequestBody final ConfirmInboundNfeReceiptRequest request) {
		final var inboundNfe = confirmInboundNfeReceiptUseCase.execute(request.toCommand(id));
		return ResponseEntity.ok(ImportSupplierNfeXmlResponse.from(inboundNfe, true));
	}

	@PostMapping("/manifestation")
	public ResponseEntity<InboundManifestationResponse> manifest(@RequestBody final ManifestInboundNfeRequest request) {
		final var manifestation = manifestInboundNfeUseCase.execute(request.toCommand());
		return ResponseEntity.status(HttpStatus.CREATED).body(InboundManifestationResponse.from(manifestation));
	}

	private boolean isSupplierResolved(final InboundNfe inboundNfe) {
		return supplierRepositoryPort.findByDocument(inboundNfe.getSupplierDocument()).isPresent();
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
