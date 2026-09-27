package br.gravita.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.sales.FollowUpRule;
import br.gravita.core.domain.sales.FollowUpRuleId;
import br.gravita.core.domain.sales.FollowUpTarget;
import br.gravita.core.domain.sales.FollowUpTask;
import br.gravita.core.domain.sales.Interaction;
import br.gravita.core.domain.sales.InteractionChannel;
import br.gravita.core.domain.sales.InteractionId;
import br.gravita.core.domain.sales.Opportunity;
import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.domain.sales.OpportunityStage;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.FollowUpRuleRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.FollowUpTaskRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.InteractionRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.OpportunityRepositoryPort;
import br.gravita.core.ports.outbound.sales.SendFollowUpAlertPort;
import br.gravita.core.usercases.sales.EvaluateFollowUpRulesService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluateFollowUpRulesServiceTest {

	@Mock
	private FollowUpRuleRepositoryPort followUpRuleRepositoryPort;

	@Mock
	private InteractionRepositoryPort interactionRepositoryPort;

	@Mock
	private OpportunityRepositoryPort opportunityRepositoryPort;

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@Mock
	private FollowUpTaskRepositoryPort followUpTaskRepositoryPort;

	@Mock
	private SendFollowUpAlertPort sendFollowUpAlertPort;

	@InjectMocks
	private EvaluateFollowUpRulesService service;

	@Test
	void notifiesTheOpportunityOwnerWhenLastInteractionIsOlderThanTheThreshold() {
		FollowUpRule rule = FollowUpRule.of(FollowUpRuleId.of(UUID.randomUUID()), 5, FollowUpTarget.OPPORTUNITY, true,
				true);
		Opportunity opportunity = anOpportunity();
		when(followUpRuleRepositoryPort.findAllActive()).thenReturn(List.of(rule));
		when(opportunityRepositoryPort.findAll()).thenReturn(List.of(opportunity));
		when(interactionRepositoryPort.findByOpportunityId(opportunity.getId()))
				.thenReturn(List.of(interactionAt(Instant.now().minus(10, ChronoUnit.DAYS))));
		when(followUpTaskRepositoryPort.save(any(FollowUpTask.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.execute();

		ArgumentCaptor<FollowUpTask> sentTask = ArgumentCaptor.forClass(FollowUpTask.class);
		verify(sendFollowUpAlertPort).send(sentTask.capture());
		assertThat(sentTask.getValue().getOpportunityId()).isEqualTo(opportunity.getId().value());
		assertThat(sentTask.getValue().getOwner()).isEqualTo(opportunity.getOwner());
		assertThat(sentTask.getValue().getDueDate()).isEqualTo(LocalDate.now());
	}

	@Test
	void doesNotNotifyWhenTheOpportunityWasContactedWithinTheThreshold() {
		FollowUpRule rule = FollowUpRule.of(FollowUpRuleId.of(UUID.randomUUID()), 5, FollowUpTarget.OPPORTUNITY, true,
				true);
		Opportunity opportunity = anOpportunity();
		when(followUpRuleRepositoryPort.findAllActive()).thenReturn(List.of(rule));
		when(opportunityRepositoryPort.findAll()).thenReturn(List.of(opportunity));
		when(interactionRepositoryPort.findByOpportunityId(opportunity.getId()))
				.thenReturn(List.of(interactionAt(Instant.now().minus(1, ChronoUnit.DAYS))));

		service.execute();

		verify(sendFollowUpAlertPort, never()).send(any());
	}

	@Test
	void treatsATargetWithNoInteractionsAtAllAsBreachingTheRule() {
		FollowUpRule rule = FollowUpRule.of(FollowUpRuleId.of(UUID.randomUUID()), 5, FollowUpTarget.OPPORTUNITY, true,
				true);
		Opportunity opportunity = anOpportunity();
		when(followUpRuleRepositoryPort.findAllActive()).thenReturn(List.of(rule));
		when(opportunityRepositoryPort.findAll()).thenReturn(List.of(opportunity));
		when(interactionRepositoryPort.findByOpportunityId(opportunity.getId())).thenReturn(List.of());
		when(followUpTaskRepositoryPort.save(any(FollowUpTask.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.execute();

		verify(sendFollowUpAlertPort).send(any(FollowUpTask.class));
	}

	@Test
	void resolvesTheOwnerOfACustomerTargetedRuleThroughTheCustomersOpportunity() {
		FollowUpRule rule = FollowUpRule.of(FollowUpRuleId.of(UUID.randomUUID()), 5, FollowUpTarget.CUSTOMER, true,
				true);
		UUID customerId = UUID.randomUUID();
		Opportunity opportunity = anOpportunityFor(customerId);
		CustomerDomain customer = CustomerDomain.builder().id(customerId).build();
		when(followUpRuleRepositoryPort.findAllActive()).thenReturn(List.of(rule));
		when(opportunityRepositoryPort.findAll()).thenReturn(List.of(opportunity));
		when(customerRepositoryPort.findAll()).thenReturn(List.of(customer));
		when(interactionRepositoryPort.findByCustomerId(customerId)).thenReturn(List.of());
		when(followUpTaskRepositoryPort.save(any(FollowUpTask.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.execute();

		ArgumentCaptor<FollowUpTask> sentTask = ArgumentCaptor.forClass(FollowUpTask.class);
		verify(sendFollowUpAlertPort).send(sentTask.capture());
		assertThat(sentTask.getValue().getCustomerId()).isEqualTo(customerId);
		assertThat(sentTask.getValue().getOwner()).isEqualTo(opportunity.getOwner());
	}

	@Test
	void skipsACustomerBreachWhenNoOpportunityResolvesAnOwner() {
		FollowUpRule rule = FollowUpRule.of(FollowUpRuleId.of(UUID.randomUUID()), 5, FollowUpTarget.CUSTOMER, true,
				true);
		UUID customerId = UUID.randomUUID();
		CustomerDomain customer = CustomerDomain.builder().id(customerId).build();
		when(followUpRuleRepositoryPort.findAllActive()).thenReturn(List.of(rule));
		when(opportunityRepositoryPort.findAll()).thenReturn(List.of());
		when(customerRepositoryPort.findAll()).thenReturn(List.of(customer));
		when(interactionRepositoryPort.findByCustomerId(customerId)).thenReturn(List.of());

		service.execute();

		verify(sendFollowUpAlertPort, never()).send(any());
	}

	@Test
	void doesNotSendADuplicateAlertForABreachAlreadyNotifiedToday() {
		FollowUpRule rule = FollowUpRule.of(FollowUpRuleId.of(UUID.randomUUID()), 5, FollowUpTarget.OPPORTUNITY, true,
				true);
		Opportunity opportunity = anOpportunity();
		when(followUpRuleRepositoryPort.findAllActive()).thenReturn(List.of(rule));
		when(opportunityRepositoryPort.findAll()).thenReturn(List.of(opportunity));
		when(interactionRepositoryPort.findByOpportunityId(opportunity.getId())).thenReturn(List.of());
		when(followUpTaskRepositoryPort.existsForTargetOnDate(opportunity.getId().value(), null, LocalDate.now()))
				.thenReturn(true);

		service.execute();

		verify(followUpTaskRepositoryPort, never()).save(any());
		verify(sendFollowUpAlertPort, never()).send(any());
	}

	@Test
	void skipsRulesThatDoNotNotifyTheOwner() {
		FollowUpRule rule = FollowUpRule.of(FollowUpRuleId.of(UUID.randomUUID()), 5, FollowUpTarget.OPPORTUNITY,
				false, true);
		when(followUpRuleRepositoryPort.findAllActive()).thenReturn(List.of(rule));

		service.execute();

		verifyNoInteractions(opportunityRepositoryPort, customerRepositoryPort, interactionRepositoryPort,
				sendFollowUpAlertPort);
	}

	private static Opportunity anOpportunity() {
		return anOpportunityFor(UUID.randomUUID());
	}

	private static Opportunity anOpportunityFor(UUID customerId) {
		return Opportunity.of(OpportunityId.of(UUID.randomUUID()), customerId, BigDecimal.TEN, 50,
				LocalDate.now().plusDays(30), UUID.randomUUID(), OpportunityStage.PROSPECTING);
	}

	private static Interaction interactionAt(Instant timestamp) {
		return Interaction.of(InteractionId.of(UUID.randomUUID()), OpportunityId.of(UUID.randomUUID()), null,
				InteractionChannel.CALL, "summary", timestamp);
	}
}
