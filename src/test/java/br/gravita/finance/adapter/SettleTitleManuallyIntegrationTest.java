package br.gravita.finance.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.AddressType;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementMethod;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.inbound.finance.SettleTitleCommand;
import br.gravita.core.ports.inbound.finance.SettleTitleManuallyUseCase;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
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
class SettleTitleManuallyIntegrationTest {

	@Autowired
	private SettleTitleManuallyUseCase settleTitleManuallyUseCase;

	@Autowired
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Autowired
	private SettlementRepositoryPort settlementRepositoryPort;

	@Autowired
	private CustomerRepositoryPort customerRepositoryPort;

	private CustomerDomain savedCustomer() {
		CustomerDomain customer = CustomerDomain.builder().name("Maria Silva")
				.documentDomain(Document.cpf("111.444.777-35")).creditLimit(new BigDecimal("1000.00"))
				.currentBalance(BigDecimal.ZERO).status(CustomerStatus.REGULAR)
				.addresses(List.of(AddressDomain.builder().type(AddressType.BILLING).street("Rua A")
						.neighborhood("Centro").city("São Paulo").state("SP").zipCode("01000-000").isDefault(true)
						.build()))
				.contacts(List.of()).priceTables(List.of()).build();
		customer.setId(UUID.randomUUID());
		return customerRepositoryPort.save(customer);
	}

	private Receivable savedReceivable(UUID customerId, LocalDate dueDate) {
		return receivableRepositoryPort.save(Receivable.createManual(ReceivableId.of(UUID.randomUUID()), customerId,
				new BigDecimal("100.00"), dueDate, null));
	}

	private SettleTitleCommand command(Receivable receivable, String amount, boolean partial) {
		return new SettleTitleCommand(receivable.getId().value(), new BigDecimal(amount), new BigDecimal("2.00"),
				null, null, null, partial);
	}

	@Test
	@DisplayName("Persists manual settlements and the receivable status across partial and full payments")
	void persistsTheManualSettlementsAndTheReceivableStatusAcrossPartialAndFullBaixas() {
		Receivable receivable = savedReceivable(UUID.randomUUID(), LocalDate.now().plusDays(30));

		Settlement partial = settleTitleManuallyUseCase.execute(command(receivable, "40.00", true));

		assertThat(partial.getMethod()).isEqualTo(SettlementMethod.MANUAL);
		assertThat(receivableRepositoryPort.findById(receivable.getId())).get().extracting(Receivable::getStatus)
				.isEqualTo(ReceivableStatus.PARTIALLY_SETTLED);

		settleTitleManuallyUseCase.execute(command(receivable, "60.00", false));

		assertThat(receivableRepositoryPort.findById(receivable.getId())).get().extracting(Receivable::getStatus)
				.isEqualTo(ReceivableStatus.SETTLED);
		assertThat(settlementRepositoryPort.findByReceivableId(receivable.getId())).hasSize(2)
				.allSatisfy(settlement -> assertThat(settlement.getInterest()).isEqualByComparingTo("2.00"));
		assertThatThrownBy(() -> settleTitleManuallyUseCase.execute(command(receivable, "1.00", true)))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Keeps the customer's credit status in masterdata up to date after a manual settlement")
	void keepsTheCustomersCreditStatusInMasterdataCurrent() {
		CustomerDomain customer = savedCustomer();
		Receivable overdue = savedReceivable(customer.getId(), LocalDate.now().minusDays(10));

		settleTitleManuallyUseCase.execute(command(overdue, "40.00", true));

		assertThat(customerRepositoryPort.get(customer.getId())).get().satisfies(updated -> {
			assertThat(updated.getStatus()).isEqualTo(CustomerStatus.DELINQUENT);
			assertThat(updated.getCurrentBalance()).isEqualByComparingTo("60.00");
		});

		settleTitleManuallyUseCase.execute(command(overdue, "60.00", false));

		assertThat(customerRepositoryPort.get(customer.getId())).get().satisfies(updated -> {
			assertThat(updated.getStatus()).isEqualTo(CustomerStatus.REGULAR);
			assertThat(updated.getCurrentBalance()).isEqualByComparingTo("0.00");
		});
	}
}
