package br.gravita.finance.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.AddressType;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Renegotiation;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.inbound.finance.RenegotiateTitleCommand;
import br.gravita.core.ports.inbound.finance.RenegotiateTitleCommand.Installment;
import br.gravita.core.ports.inbound.finance.RenegotiateTitleUseCase;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.RenegotiationRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RenegotiateTitleIntegrationTest {

	@Autowired
	private RenegotiateTitleUseCase renegotiateTitleUseCase;

	@Autowired
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Autowired
	private RenegotiationRepositoryPort renegotiationRepositoryPort;

	@Autowired
	private CustomerRepositoryPort customerRepositoryPort;

	private CustomerDomain savedCustomer(CustomerStatus status) {
		CustomerDomain customer = CustomerDomain.builder()
				.name("Maria Silva")
				.documentDomain(Document.cpf("111.444.777-35"))
				.creditLimit(new BigDecimal("1000.00"))
				.currentBalance(new BigDecimal("100.00"))
				.status(status)
				.addresses(List.of(AddressDomain.builder().type(AddressType.BILLING).street("Rua A")
						.neighborhood("Centro").city("São Paulo").state("SP").zipCode("01000-000").isDefault(true)
						.build()))
				.contacts(List.of())
				.priceTables(List.of())
				.build();
		customer.setId(UUID.randomUUID());
		return customerRepositoryPort.save(customer);
	}

	private Receivable savedOverdue(UUID customerId, String amount) {
		return receivableRepositoryPort.save(Receivable.of(ReceivableId.of(UUID.randomUUID()), customerId,
				ReceivableOrigin.MANUAL, new BigDecimal(amount), LocalDate.now().minusDays(15), null,
				ReceivableStatus.OPEN, null, null));
	}

	private RenegotiateTitleCommand twoInstallmentsFor(Receivable... originals) {
		return new RenegotiateTitleCommand(
				List.of(originals).stream().map(r -> r.getId().value()).toList(),
				List.of(new Installment(LocalDate.now().plusDays(30), new BigDecimal("60.00")),
						new Installment(LocalDate.now().plusDays(60), new BigDecimal("60.00"))));
	}

	@Test
	@DisplayName("Persists the renegotiated original titles, the new open titles and the link between them")
	void persistsTheRenegotiatedOriginalsTheNewOpenTitlesAndTheLinkBetweenThem() {
		CustomerDomain customer = savedCustomer(CustomerStatus.DELINQUENT);
		Receivable original = savedOverdue(customer.getId(), "100.00");

		Renegotiation renegotiation = renegotiateTitleUseCase.execute(twoInstallmentsFor(original));

		assertThat(receivableRepositoryPort.findById(original.getId())).get().extracting(Receivable::getStatus)
				.isEqualTo(ReceivableStatus.RENEGOTIATED);
		assertThat(renegotiation.getNewReceivableIds()).hasSize(2).allSatisfy(id -> assertThat(
				receivableRepositoryPort.findById(id)).get().satisfies(created -> {
					assertThat(created.getStatus()).isEqualTo(ReceivableStatus.OPEN);
					assertThat(created.getOrigin()).isEqualTo(ReceivableOrigin.RENEGOTIATION);
					assertThat(created.getCustomerId()).isEqualTo(customer.getId());
				}));

		Renegotiation stored = renegotiationRepositoryPort.findByOriginalReceivableId(original.getId())
				.orElseThrow();
		assertThat(stored.getId()).isEqualTo(renegotiation.getId());
		assertThat(stored.getCustomerId()).isEqualTo(customer.getId());
		assertThat(stored.getOriginalReceivableIds()).containsExactly(original.getId());
		assertThat(stored.getNewReceivableIds()).containsExactlyElementsOf(renegotiation.getNewReceivableIds());
	}

	@Test
	@DisplayName("Removes a renegotiated title from the set of outstanding titles")
	void theRenegotiatedTitleLeavesTheOutstandingSet() {
		CustomerDomain customer = savedCustomer(CustomerStatus.DELINQUENT);
		Receivable original = savedOverdue(customer.getId(), "100.00");

		renegotiateTitleUseCase.execute(twoInstallmentsFor(original));

		assertThat(receivableRepositoryPort.findUnsettledByCustomerId(customer.getId()))
				.extracting(Receivable::getId).doesNotContain(original.getId()).hasSize(2);
	}

	@Test
	@DisplayName("Clears the customer's delinquency and updates its balance after renegotiation")
	void clearsTheCustomersDelinquencyAndUpdatesItsBalance() {
		CustomerDomain customer = savedCustomer(CustomerStatus.DELINQUENT);
		Receivable original = savedOverdue(customer.getId(), "100.00");

		renegotiateTitleUseCase.execute(twoInstallmentsFor(original));

		CustomerDomain updated = customerRepositoryPort.get(customer.getId()).orElseThrow();
		assertThat(updated.getStatus()).isEqualTo(CustomerStatus.REGULAR);
		assertThat(updated.getCurrentBalance()).isEqualByComparingTo("120.00");
	}

	@Test
	@DisplayName("Rejects renegotiating a title that was already renegotiated")
	void aTitleCanOnlyBeRenegotiatedOnce() {
		CustomerDomain customer = savedCustomer(CustomerStatus.DELINQUENT);
		Receivable original = savedOverdue(customer.getId(), "100.00");
		renegotiateTitleUseCase.execute(twoInstallmentsFor(original));

		assertThatThrownBy(() -> renegotiateTitleUseCase.execute(twoInstallmentsFor(original)))
				.isInstanceOf(BusinessRuleException.class);
	}
}
