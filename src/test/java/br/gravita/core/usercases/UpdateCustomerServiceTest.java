package br.gravita.core.usercases;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.AddressType;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerPriceTableLink;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.inbound.masterdata.UpdateCustomerCommand;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateCustomerServiceTest {

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	private CustomerDomain existingCustomer(UUID id) {
		return CustomerDomain.builder()
				.id(id)
				.version(3L)
				.name("Maria Silva")
				.documentDomain(Document.cpf("111.444.777-35"))
				.email("maria@example.com")
				.creditLimit(new BigDecimal("1000.00"))
				.currentBalance(new BigDecimal("250.00"))
				.status(CustomerStatus.REGULAR)
				.addresses(List.of(billingAddress()))
				.contacts(List.of())
				.priceTables(List.of(CustomerPriceTableLink.builder().priceTableId(UUID.randomUUID()).priority(1).build()))
				.build();
	}

	@DisplayName("A partial update keeps the customer fields that were not specified")
	@Test
	void shouldKeepUnspecifiedFieldsOnPartialUpdate() {
		UpdateCustomerService service = new UpdateCustomerService(customerRepositoryPort);
		UUID id = UUID.randomUUID();
		when(customerRepositoryPort.get(id)).thenReturn(Optional.of(existingCustomer(id)));
		when(customerRepositoryPort.save(any(CustomerDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UpdateCustomerCommand command = new UpdateCustomerCommand(
				id, null, "Maria S. Costa", null, null, null, null, null, null, null, null, null);

		service.execute(command);

		ArgumentCaptor<CustomerDomain> saved = ArgumentCaptor.forClass(CustomerDomain.class);
		verify(customerRepositoryPort).save(saved.capture());
		CustomerDomain updated = saved.getValue();
		assertThat(updated.getName()).isEqualTo("Maria S. Costa");
		assertThat(updated.getEmail()).isEqualTo("maria@example.com");
		assertThat(updated.getStatus()).isEqualTo(CustomerStatus.REGULAR);
		assertThat(updated.getCurrentBalance()).isEqualByComparingTo("250.00");
		assertThat(updated.getPriceTables()).hasSize(1);
		assertThat(updated.getVersion()).isEqualTo(3L);
	}

	@DisplayName("Re-validates the fiscal document when the update changes it")
	@Test
	void shouldReValidateDocumentWhenChanged() {
		UpdateCustomerService service = new UpdateCustomerService(customerRepositoryPort);
		UUID id = UUID.randomUUID();
		when(customerRepositoryPort.get(id)).thenReturn(Optional.of(existingCustomer(id)));

		Document newCnpj = Document.cnpj("11.222.333/0001-81");
		UpdateCustomerCommand command = new UpdateCustomerCommand(
				id, newCnpj, null, null, null, null, null, null, null, null, null, null);

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class);
	}

	@DisplayName("Leaves the credit status and balance untouched when the update omits them")
	@Test
	void shouldNotClobberStatusOrBalanceLeftUnspecified() {
		UpdateCustomerService service = new UpdateCustomerService(customerRepositoryPort);
		UUID id = UUID.randomUUID();
		when(customerRepositoryPort.get(id)).thenReturn(Optional.of(existingCustomer(id)));
		when(customerRepositoryPort.save(any(CustomerDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UpdateCustomerCommand command = new UpdateCustomerCommand(
				id, null, null, "new-email@example.com", null, null, null, null, null, null, null, null);

		service.execute(command);

		ArgumentCaptor<CustomerDomain> saved = ArgumentCaptor.forClass(CustomerDomain.class);
		verify(customerRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(CustomerStatus.REGULAR);
		assertThat(saved.getValue().getCurrentBalance()).isEqualByComparingTo("250.00");
	}

	@DisplayName("Throws not found when updating a customer that does not exist")
	@Test
	void shouldThrowWhenCustomerDoesNotExist() {
		UpdateCustomerService service = new UpdateCustomerService(customerRepositoryPort);
		UUID id = UUID.randomUUID();
		when(customerRepositoryPort.get(id)).thenReturn(Optional.empty());

		UpdateCustomerCommand command = new UpdateCustomerCommand(
				id, null, "New Name", null, null, null, null, null, null, null, null, null);

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(ResourceNotFoundException.class);
	}

	private AddressDomain billingAddress() {
		return AddressDomain.builder().type(AddressType.BILLING).street("Rua A").neighborhood("Centro")
				.city("São Paulo").state("SP").zipCode("01000-000").isDefault(true).build();
	}
}
