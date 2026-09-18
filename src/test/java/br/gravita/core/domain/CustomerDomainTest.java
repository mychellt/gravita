package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerDomainTest {

	private final DocumentDomain cpf = DocumentDomain.cpf("111.444.777-35");
	private final DocumentDomain cnpj = DocumentDomain.cnpj("11.222.333/0001-81");

	@Test
	void shouldExposeCustomerData() {
		CustomerDomain customer = customerWithStatus(CustomerStatus.REGULAR);

		assertThat(customer.getStatus()).isEqualTo(CustomerStatus.REGULAR);
		assertThat(customer.getName()).isEqualTo("Maria Silva");
		assertThat(customer.getDocumentDomain()).isEqualTo(cpf);
	}

	@Test
	void shouldNotAllowBlockingAlreadyBlockedCustomer() {
		CustomerDomain customer = customerWithStatus(CustomerStatus.REGULAR);
		customer.block();

		assertThatThrownBy(customer::block).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldReactivateBlockedCustomer() {
		CustomerDomain customer = customerWithStatus(CustomerStatus.BLOCKED);

		customer.reactivate();

		assertThat(customer.getStatus()).isEqualTo(CustomerStatus.REGULAR);
	}

	@Test
	void shouldValidateSuccessfullyWithOneDefaultAddressPerType() {
		CustomerDomain customer = baseCustomer(cpf, List.of(billingAddress(true), deliveryAddress(true)));

		assertThatCode(customer::validateForRegistration).doesNotThrowAnyException();
	}

	@Test
	void shouldRequireIeIndicatorAndFinalConsumerForCompanyCustomer() {
		CustomerDomain customer = baseCustomer(cnpj, List.of(billingAddress(true)));

		assertThatThrownBy(customer::validateForRegistration).isInstanceOf(BusinessRuleException.class);

		customer.setIeIndicator(IeIndicator.TAXPAYER);
		assertThatThrownBy(customer::validateForRegistration).isInstanceOf(BusinessRuleException.class);

		customer.setFinalConsumer(false);
		assertThatCode(customer::validateForRegistration).doesNotThrowAnyException();
	}

	@Test
	void shouldNotRequireIeIndicatorForIndividualCustomer() {
		CustomerDomain customer = baseCustomer(cpf, List.of(billingAddress(true)));

		assertThatCode(customer::validateForRegistration).doesNotThrowAnyException();
	}

	@Test
	void shouldRequireAtLeastOneAddress() {
		CustomerDomain customer = baseCustomer(cpf, List.of());

		assertThatThrownBy(customer::validateForRegistration).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldRequireExactlyOneDefaultAddressPerType() {
		CustomerDomain noDefault = baseCustomer(cpf, List.of(billingAddress(false)));
		assertThatThrownBy(noDefault::validateForRegistration).isInstanceOf(BusinessRuleException.class);

		CustomerDomain twoDefaults = baseCustomer(cpf, List.of(billingAddress(true), billingAddress(true)));
		assertThatThrownBy(twoDefaults::validateForRegistration).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldRequireUniquePriceTablePriorities() {
		CustomerDomain customer = baseCustomer(cpf, List.of(billingAddress(true)));
		customer.setPriceTables(List.of(
				CustomerPriceTableLink.builder().priceTableId(UUID.randomUUID()).priority(1).build(),
				CustomerPriceTableLink.builder().priceTableId(UUID.randomUUID()).priority(1).build()));

		assertThatThrownBy(customer::validateForRegistration).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void shouldAllowExplicitlyOrderedPriceTables() {
		CustomerDomain customer = baseCustomer(cpf, List.of(billingAddress(true)));
		customer.setPriceTables(List.of(
				CustomerPriceTableLink.builder().priceTableId(UUID.randomUUID()).priority(1).build(),
				CustomerPriceTableLink.builder().priceTableId(UUID.randomUUID()).priority(2).build()));

		assertThatCode(customer::validateForRegistration).doesNotThrowAnyException();
	}

	private CustomerDomain customerWithStatus(CustomerStatus status) {
		return CustomerDomain.builder()
				.name("Maria Silva")
				.documentDomain(cpf)
				.email("maria@example.com")
				.status(status)
				.build();
	}

	private CustomerDomain baseCustomer(DocumentDomain document, List<AddressDomain> addresses) {
		return CustomerDomain.builder()
				.name("Maria Silva")
				.documentDomain(document)
				.addresses(addresses)
				.build();
	}

	private AddressDomain billingAddress(boolean isDefault) {
		return AddressDomain.builder().type(AddressType.BILLING).street("Rua A").neighborhood("Centro")
				.city("São Paulo").state("SP").zipCode("01000-000").isDefault(isDefault).build();
	}

	private AddressDomain deliveryAddress(boolean isDefault) {
		return AddressDomain.builder().type(AddressType.DELIVERY).street("Rua B").neighborhood("Centro")
				.city("São Paulo").state("SP").zipCode("01000-000").isDefault(isDefault).build();
	}
}
