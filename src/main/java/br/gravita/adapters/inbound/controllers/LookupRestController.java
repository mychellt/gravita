package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.response.PersonLookupResponse;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.LookupPersonByDocumentQuery;
import br.gravita.core.domain.PersonLookupResult;
import br.gravita.core.ports.business.LookupPersonByDocumentPort;
import br.gravita.shared.BusinessRuleException;
import br.gravita.shared.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Always answers 200 with a best-effort (possibly empty) result: a failed or invalid
 * lookup must degrade to manual entry on the registration form, never block it.
 */
@RestController
@RequestMapping("/api/lookup")
public class LookupRestController {

	private final LookupPersonByDocumentPort lookupPersonByDocumentPort;

	public LookupRestController(LookupPersonByDocumentPort lookupPersonByDocumentPort) {
		this.lookupPersonByDocumentPort = lookupPersonByDocumentPort;
	}

	@GetMapping("/cnpj/{cnpj}")
	public ResponseEntity<PersonLookupResponse> lookupByCnpj(@PathVariable String cnpj) {
		Document document;
		try {
			document = Document.cnpj(cnpj);
		} catch (BusinessRuleException e) {
			return ResponseEntity.badRequest().build();
		}
		PersonLookupResult result = lookupPersonByDocumentPort.execute(
				new Context(LookupPersonByDocumentQuery.byDocument(document)));
		return ResponseEntity.ok(PersonLookupResponse.from(result));
	}

	@GetMapping("/cep/{cep}")
	public ResponseEntity<PersonLookupResponse> lookupByCep(@PathVariable String cep) {
		PersonLookupResult result = lookupPersonByDocumentPort.execute(
				new Context(LookupPersonByDocumentQuery.byCep(cep)));
		return ResponseEntity.ok(PersonLookupResponse.from(result));
	}
}
