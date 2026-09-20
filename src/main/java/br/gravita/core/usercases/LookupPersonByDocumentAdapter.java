package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.LookupPersonByDocumentQuery;
import br.gravita.core.domain.PersonLookupResult;
import br.gravita.core.ports.business.LookupPersonByDocumentPort;
import br.gravita.core.ports.integration.CepLookupPort;
import br.gravita.core.ports.integration.CnpjLookupPort;
import org.springframework.stereotype.Component;

@Component
public class LookupPersonByDocumentAdapter implements LookupPersonByDocumentPort {

	private final CnpjLookupPort cnpjLookupPort;
	private final CepLookupPort cepLookupPort;

	public LookupPersonByDocumentAdapter(CnpjLookupPort cnpjLookupPort, CepLookupPort cepLookupPort) {
		this.cnpjLookupPort = cnpjLookupPort;
		this.cepLookupPort = cepLookupPort;
	}

	@Override
	public PersonLookupResult execute(Context context) {
		LookupPersonByDocumentQuery query = context.getData(LookupPersonByDocumentQuery.class);
		if (query.isDocumentQuery()) {
			return cnpjLookupPort.execute(new Context(query.document())).orElseGet(PersonLookupResult::notFound);
		}
		return cepLookupPort.execute(new Context(query.cep()))
				.map(address -> new PersonLookupResult(null, address))
				.orElseGet(PersonLookupResult::notFound);
	}
}
