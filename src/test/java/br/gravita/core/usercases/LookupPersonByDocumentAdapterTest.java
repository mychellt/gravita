package br.gravita.core.usercases;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.DocumentDomain;
import br.gravita.core.domain.LookupPersonByDocumentQuery;
import br.gravita.core.domain.PersonLookupResult;
import br.gravita.core.ports.integration.CepLookupPort;
import br.gravita.core.ports.integration.CnpjLookupPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LookupPersonByDocumentAdapterTest {

	@Mock
	private CnpjLookupPort cnpjLookupPort;

	@Mock
	private CepLookupPort cepLookupPort;

	@Test
	void shouldReturnNameAndAddressForCnpjQuery() {
		LookupPersonByDocumentAdapter adapter = new LookupPersonByDocumentAdapter(cnpjLookupPort, cepLookupPort);
		DocumentDomain cnpj = DocumentDomain.cnpj("11444777000161");
		AddressDomain address = AddressDomain.builder()
				.street("Rua A").number("10").neighborhood("Centro").city("São Paulo").state("SP").zipCode("01000-000")
				.build();
		when(cnpjLookupPort.lookup(cnpj)).thenReturn(Optional.of(new PersonLookupResult("Acme LTDA", address)));

		PersonLookupResult result = adapter.execute(new Context(LookupPersonByDocumentQuery.byDocument(cnpj)));

		assertThat(result.name()).isEqualTo("Acme LTDA");
		assertThat(result.address()).isEqualTo(address);
		verifyNoInteractions(cepLookupPort);
	}

	@Test
	void shouldReturnAddressOnlyForCepQuery() {
		LookupPersonByDocumentAdapter adapter = new LookupPersonByDocumentAdapter(cnpjLookupPort, cepLookupPort);
		AddressDomain address = AddressDomain.builder()
				.street("Rua B").neighborhood("Bairro").city("Rio de Janeiro").state("RJ").zipCode("20000-000")
				.build();
		when(cepLookupPort.lookup("20000000")).thenReturn(Optional.of(address));

		PersonLookupResult result = adapter.execute(new Context(LookupPersonByDocumentQuery.byCep("20000000")));

		assertThat(result.name()).isNull();
		assertThat(result.address()).isEqualTo(address);
		verifyNoInteractions(cnpjLookupPort);
	}

	@Test
	void shouldDegradeToEmptyResultWhenCnpjLookupFails() {
		LookupPersonByDocumentAdapter adapter = new LookupPersonByDocumentAdapter(cnpjLookupPort, cepLookupPort);
		DocumentDomain cnpj = DocumentDomain.cnpj("11444777000161");
		when(cnpjLookupPort.lookup(cnpj)).thenReturn(Optional.empty());

		PersonLookupResult result = adapter.execute(new Context(LookupPersonByDocumentQuery.byDocument(cnpj)));

		assertThat(result.name()).isNull();
		assertThat(result.address()).isNull();
	}

	@Test
	void shouldDegradeToEmptyResultWhenCepLookupFails() {
		LookupPersonByDocumentAdapter adapter = new LookupPersonByDocumentAdapter(cnpjLookupPort, cepLookupPort);
		when(cepLookupPort.lookup("99999999")).thenReturn(Optional.empty());

		PersonLookupResult result = adapter.execute(new Context(LookupPersonByDocumentQuery.byCep("99999999")));

		assertThat(result.name()).isNull();
		assertThat(result.address()).isNull();
	}
}
