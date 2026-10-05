package br.gravita.core.usercases;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.LookupPersonByDocumentQuery;
import br.gravita.core.domain.PersonLookupResult;
import br.gravita.core.ports.integration.CepLookupPort;
import br.gravita.core.ports.integration.CnpjLookupPort;
import br.gravita.core.domain.shared.Document;
import org.junit.jupiter.api.DisplayName;
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

	@DisplayName("A CNPJ lookup returns the company name and address")
	@Test
	void shouldReturnNameAndAddressForCnpjQuery() {
		final LookupPersonByDocumentAdapter adapter = new LookupPersonByDocumentAdapter(cnpjLookupPort, cepLookupPort);
		final Document cnpj = Document.cnpj("11444777000161");
		final AddressDomain address = AddressDomain.builder()
				.street("Rua A").number("10").neighborhood("Centro").city("São Paulo").state("SP").zipCode("01000-000")
				.build();
		when(cnpjLookupPort.execute(new Context(cnpj))).thenReturn(Optional.of(new PersonLookupResult("Acme LTDA", address)));

		final PersonLookupResult result = adapter.execute(new Context(LookupPersonByDocumentQuery.byDocument(cnpj)));

		assertThat(result.name()).isEqualTo("Acme LTDA");
		assertThat(result.address()).isEqualTo(address);
		verifyNoInteractions(cepLookupPort);
	}

	@DisplayName("A CEP lookup returns the address only")
	@Test
	void shouldReturnAddressOnlyForCepQuery() {
		final LookupPersonByDocumentAdapter adapter = new LookupPersonByDocumentAdapter(cnpjLookupPort, cepLookupPort);
		final AddressDomain address = AddressDomain.builder()
				.street("Rua B").neighborhood("Bairro").city("Rio de Janeiro").state("RJ").zipCode("20000-000")
				.build();
		when(cepLookupPort.execute(new Context("20000000"))).thenReturn(Optional.of(address));

		final PersonLookupResult result = adapter.execute(new Context(LookupPersonByDocumentQuery.byCep("20000000")));

		assertThat(result.name()).isNull();
		assertThat(result.address()).isEqualTo(address);
		verifyNoInteractions(cnpjLookupPort);
	}

	@DisplayName("Degrades to an empty result when the CNPJ lookup provider fails")
	@Test
	void shouldDegradeToEmptyResultWhenCnpjLookupFails() {
		final LookupPersonByDocumentAdapter adapter = new LookupPersonByDocumentAdapter(cnpjLookupPort, cepLookupPort);
		final Document cnpj = Document.cnpj("11444777000161");
		when(cnpjLookupPort.execute(new Context(cnpj))).thenReturn(Optional.empty());

		final PersonLookupResult result = adapter.execute(new Context(LookupPersonByDocumentQuery.byDocument(cnpj)));

		assertThat(result.name()).isNull();
		assertThat(result.address()).isNull();
	}

	@DisplayName("Degrades to an empty result when the CEP lookup provider fails")
	@Test
	void shouldDegradeToEmptyResultWhenCepLookupFails() {
		final LookupPersonByDocumentAdapter adapter = new LookupPersonByDocumentAdapter(cnpjLookupPort, cepLookupPort);
		when(cepLookupPort.execute(new Context("99999999"))).thenReturn(Optional.empty());

		final PersonLookupResult result = adapter.execute(new Context(LookupPersonByDocumentQuery.byCep("99999999")));

		assertThat(result.name()).isNull();
		assertThat(result.address()).isNull();
	}
}
