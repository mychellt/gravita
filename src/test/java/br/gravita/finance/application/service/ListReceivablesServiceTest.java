package br.gravita.finance.application.service;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.finance.ReceivableListItem;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.usercases.CallerCompanyResolver;
import br.gravita.core.usercases.finance.ListReceivablesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListReceivablesServiceTest {

	@Mock
	private ReceivableRepositoryPort receivableRepositoryPort;
	@Mock
	private SettlementRepositoryPort settlementRepositoryPort;
	@Mock
	private CustomerRepositoryPort customerRepositoryPort;
	@Mock
	private UserRepositoryPort userRepositoryPort;

	private ListReceivablesService service;
	private final UserId callerId = UserId.generate();
	private final UUID companyId = UUID.randomUUID();
	private final UUID customerId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new ListReceivablesService(receivableRepositoryPort, settlementRepositoryPort, customerRepositoryPort,
				new CallerCompanyResolver(userRepositoryPort));
	}

	@Test
	@DisplayName("Lists the receivables of the caller's company customers with the customer name and the balance still owed")
	void listsTheCompanysReceivablesWithNameAndBalance() {
		final Receivable receivable = Receivable.createManual(ReceivableId.of(UUID.randomUUID()), customerId, new BigDecimal("100.00"),
				LocalDate.of(2026, 10, 1), 1);
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().companyId(companyId).build()));
		when(customerRepositoryPort.findAllByCompanyId(companyId))
				.thenReturn(List.of(CustomerDomain.builder().id(customerId).name("Cliente Alfa").build()));
		when(receivableRepositoryPort.findByCustomerIds(Set.of(customerId))).thenReturn(List.of(receivable));
		when(settlementRepositoryPort.findByReceivableIds(List.of(receivable.getId()))).thenReturn(List.of(Settlement.manual(
				SettlementId.of(UUID.randomUUID()), receivable.getId(), new BigDecimal("30.00"), BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, BigDecimal.ZERO, Instant.now())));

		final List<ReceivableListItem> result = service.execute(new Context().withCaller(callerId));

		assertThat(result).hasSize(1);
		assertThat(result.get(0).customerName()).isEqualTo("Cliente Alfa");
		assertThat(result.get(0).remainingBalance()).isEqualByComparingTo("70.00");
	}

	@Test
	@DisplayName("A caller without a company sees nothing and nothing is read")
	void callerWithoutCompanySeesNothing() {
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().build()));

		assertThat(service.execute(new Context().withCaller(callerId))).isEmpty();
		verify(receivableRepositoryPort, never()).findByCustomerIds(any());
	}
}
