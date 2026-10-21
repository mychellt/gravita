package br.gravita.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.sales.Opportunity;
import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.domain.sales.OpportunityStage;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.sales.ListOpportunitiesQuery;
import br.gravita.core.ports.inbound.sales.OpportunityListItem;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.OpportunityRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.usercases.sales.ListOpportunitiesService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListOpportunitiesServiceTest {

	private final OpportunityRepositoryPort opportunities = mock(OpportunityRepositoryPort.class);
	private final CustomerRepositoryPort customers = mock(CustomerRepositoryPort.class);
	private final UserRepositoryPort users = mock(UserRepositoryPort.class);
	private final ListOpportunitiesService service = new ListOpportunitiesService(opportunities, customers, users);

	private Opportunity opportunity(final UUID customerId, final UUID owner, final OpportunityStage stage) {
		return Opportunity.of(OpportunityId.of(UUID.randomUUID()), customerId, BigDecimal.TEN, 50,
				LocalDate.of(2026, 12, 1), owner, stage);
	}

	@Test
	@DisplayName("names the customer and owner of each opportunity, looking each one up once")
	void namesCustomerAndOwner() {
		final UUID customerId = UUID.randomUUID();
		final UUID ownerId = UUID.randomUUID();
		final CustomerDomain customer = mock(CustomerDomain.class);
		when(customer.getName()).thenReturn("Construtora Alfa");
		final User owner = mock(User.class);
		when(owner.getName()).thenReturn("Ricardo");
		when(customers.get(customerId)).thenReturn(Optional.of(customer));
		when(users.findById(UserId.of(ownerId))).thenReturn(Optional.of(owner));
		when(opportunities.findAll()).thenReturn(List.of(
				opportunity(customerId, ownerId, OpportunityStage.PROPOSAL),
				opportunity(customerId, ownerId, OpportunityStage.LOST)));

		final List<OpportunityListItem> result = service.execute(new ListOpportunitiesQuery(null));

		assertThat(result).extracting(OpportunityListItem::customerName).containsOnly("Construtora Alfa");
		assertThat(result).extracting(OpportunityListItem::ownerName).containsOnly("Ricardo");
		verify(customers, times(1)).get(customerId);
	}

	@Test
	@DisplayName("leaves the names null when the customer or owner no longer exists, and filters by stage")
	void unknownNames() {
		when(customers.get(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());
		when(users.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());
		when(opportunities.findByStage(OpportunityStage.CLOSED))
				.thenReturn(List.of(opportunity(UUID.randomUUID(), UUID.randomUUID(), OpportunityStage.CLOSED)));

		final List<OpportunityListItem> result = service.execute(new ListOpportunitiesQuery(OpportunityStage.CLOSED));

		assertThat(result).hasSize(1);
		assertThat(result.get(0).customerName()).isNull();
		assertThat(result.get(0).ownerName()).isNull();
	}
}
