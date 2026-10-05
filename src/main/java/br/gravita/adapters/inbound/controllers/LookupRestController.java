package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.response.PersonLookupResponse;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.LookupPersonByDocumentQuery;
import br.gravita.core.domain.PersonLookupResult;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.business.LookupPersonByDocumentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/lookup")
public class LookupRestController {

    private final LookupPersonByDocumentPort lookupPersonByDocumentPort;

    @GetMapping("/cnpj/{cnpj}")
    public ResponseEntity<PersonLookupResponse> lookupByCnpj(@PathVariable final String cnpj) {
        final Document document;
        try {
            document = Document.cnpj(cnpj);
        } catch (final BusinessRuleException e) {
            return ResponseEntity.badRequest().build();
        }
        final PersonLookupResult result = lookupPersonByDocumentPort.execute(
                new Context(LookupPersonByDocumentQuery.byDocument(document)));
        return ResponseEntity.ok(PersonLookupResponse.from(result));
    }

    @GetMapping("/cep/{cep}")
    public ResponseEntity<PersonLookupResponse> lookupByCep(@PathVariable final String cep) {
        final PersonLookupResult result = lookupPersonByDocumentPort.execute(
                new Context(LookupPersonByDocumentQuery.byCep(cep)));
        return ResponseEntity.ok(PersonLookupResponse.from(result));
    }
}
