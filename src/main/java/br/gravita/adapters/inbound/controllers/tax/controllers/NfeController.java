package br.gravita.adapters.inbound.controllers.tax.controllers;

import br.gravita.adapters.inbound.controllers.tax.dtos.CancelNfeRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.CancelNfeResponse;
import br.gravita.adapters.inbound.controllers.tax.dtos.CorrectionLetterResponse;
import br.gravita.adapters.inbound.controllers.tax.dtos.IssueCorrectionLetterRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.IssueNfeRequest;
import br.gravita.adapters.inbound.controllers.tax.dtos.IssueNfeResponse;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.CorrectionLetter;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.ports.inbound.tax.CancelNfeCommand;
import br.gravita.core.ports.inbound.tax.CancelNfeUseCase;
import br.gravita.core.ports.inbound.tax.IssueCorrectionLetterCommand;
import br.gravita.core.ports.inbound.tax.IssueCorrectionLetterUseCase;
import br.gravita.core.ports.inbound.tax.IssueNfeUseCase;
import br.gravita.core.ports.inbound.tax.ResendNfeEmailCommand;
import br.gravita.core.ports.inbound.tax.ResendNfeEmailUseCase;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.XmlObjectStoragePort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.DanfeOrientation;
import br.gravita.core.ports.outbound.tax.GenerateDanfePort;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/nfe")
public class NfeController {

	private final IssueNfeUseCase issueNfeUseCase;
	private final ResendNfeEmailUseCase resendNfeEmailUseCase;
	private final IssueCorrectionLetterUseCase issueCorrectionLetterUseCase;
	private final CancelNfeUseCase cancelNfeUseCase;
	private final NfeRepositoryPort nfeRepositoryPort;
	private final CompanyRepositoryPort companyRepositoryPort;
	private final XmlObjectStoragePort xmlObjectStoragePort;
	private final GenerateDanfePort generateDanfePort;

	@PostMapping
	public ResponseEntity<IssueNfeResponse> issue(@Valid @RequestBody IssueNfeRequest request) {
		NfeDocument document = issueNfeUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/nfe/" + document.getId().value()))
				.body(IssueNfeResponse.from(document));
	}

	/**
	 * UC-M2-03 (AC4/AC6): the portrait DANFE stored at authorization time is
	 * served as-is; a landscape copy is rendered on demand rather than stored
	 * twice per document.
	 */
	@GetMapping(value = "/{id}/danfe", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> danfe(@PathVariable UUID id,
			@RequestParam(defaultValue = "PORTRAIT") DanfeOrientation orientation) {
		NfeDocument document = authorizedDocument(id);
		byte[] content = orientation == DanfeOrientation.PORTRAIT
				? xmlObjectStoragePort.retrieve(document.getDanfeStorageRef())
				: generateDanfePort.generate(document, resolveCompany(document), DanfeOrientation.LANDSCAPE);
		return ResponseEntity.ok().body(content);
	}

	@GetMapping(value = "/{id}/xml", produces = MediaType.APPLICATION_XML_VALUE)
	public ResponseEntity<byte[]> xml(@PathVariable UUID id) {
		NfeDocument document = authorizedDocument(id);
		return ResponseEntity.ok().body(xmlObjectStoragePort.retrieve(document.getXmlStorageRef()));
	}

	@PostMapping("/{id}/resend-email")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void resendEmail(@PathVariable UUID id) {
		resendNfeEmailUseCase.execute(new ResendNfeEmailCommand(id));
	}

	@PostMapping("/{id}/correction-letters")
	@ResponseStatus(HttpStatus.CREATED)
	public CorrectionLetterResponse issueCorrectionLetter(@PathVariable UUID id,
			@Valid @RequestBody IssueCorrectionLetterRequest request) {
		CorrectionLetter correctionLetter = issueCorrectionLetterUseCase
				.execute(new IssueCorrectionLetterCommand(id, request.text()));
		return CorrectionLetterResponse.from(correctionLetter);
	}

	@PostMapping("/{id}/cancel")
	public CancelNfeResponse cancel(@PathVariable UUID id, @Valid @RequestBody CancelNfeRequest request) {
		NfeDocument document = cancelNfeUseCase.execute(new CancelNfeCommand(id, request.justification()));
		return CancelNfeResponse.from(document);
	}

	private NfeDocument authorizedDocument(UUID id) {
		NfeDocument document = nfeRepositoryPort.findById(NfeDocumentId.of(id))
				.orElseThrow(() -> new ResourceNotFoundException("NfeDocument not found: " + id));
		if (document.getStatus() != NfeDocumentStatus.AUTHORIZED) {
			throw new BusinessRuleException("NfeDocument " + id + " is not AUTHORIZED (current status: "
					+ document.getStatus() + ")");
		}
		return document;
	}

	private Company resolveCompany(NfeDocument document) {
		CompanyId companyId = document.getIssuerCompanyId();
		return companyRepositoryPort.findById(companyId)
				.orElseThrow(() -> new BusinessRuleException("Company not found: " + companyId.value()));
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}
}
