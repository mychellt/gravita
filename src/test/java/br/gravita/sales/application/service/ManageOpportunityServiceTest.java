package br.gravita.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.sales.Opportunity;
import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.domain.sales.OpportunityNotFoundException;
import br.gravita.core.domain.sales.OpportunityStage;
import br.gravita.core.domain.sales.StageTransition;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.ChangeOpportunityStageCommand;
import br.gravita.core.ports.inbound.sales.CreateOpportunityCommand;
import br.gravita.core.ports.inbound.sales.OpportunityView;
import br.gravita.core.ports.inbound.sales.UpdateOpportunityCommand;
import br.gravita.core.ports.outbound.persistence.sales.OpportunityRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.StageTransitionRepositoryPort;
import br.gravita.core.usercases.sales.ManageOpportunityService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManageOpportunityServiceTest {

	@Mock
	private OpportunityRepositoryPort opportunityRepositoryPort;

	@Mock
	private StageTransitionRepositoryPort stageTransitionRepositoryPort;

	@InjectMocks
	private ManageOpportunityService service;

	@Test
	@DisplayName("Creates an opportunity in the PROSPECTING stage")
	void createsAnOpportunityInTheProspectingStage() {
		when(opportunityRepositoryPort.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final CreateOpportunityCommand command = new CreateOpportunityCommand(UUID.randomUUID(), BigDecimal.TEN, 50,
				LocalDate.now().plusDays(30), UUID.randomUUID());

		final OpportunityView view = service.create(command);

		assertThat(view.stage()).isEqualTo(OpportunityStage.PROSPECTING);
		verify(stageTransitionRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Changing the stage appends exactly one stage transition")
	void changingStageAppendsExactlyOneStageTransition() {
		final OpportunityId id = OpportunityId.of(UUID.randomUUID());
		when(opportunityRepositoryPort.findById(id)).thenReturn(Optional.of(opportunity(id, OpportunityStage.PROSPECTING)));
		when(opportunityRepositoryPort.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(stageTransitionRepositoryPort.save(any(StageTransition.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final OpportunityView view = service.changeStage(
				new ChangeOpportunityStageCommand(id, OpportunityStage.NEGOTIATION));

		assertThat(view.stage()).isEqualTo(OpportunityStage.NEGOTIATION);
		final ArgumentCaptor<StageTransition> transition = ArgumentCaptor.forClass(StageTransition.class);
		verify(stageTransitionRepositoryPort).save(transition.capture());
		assertThat(transition.getValue().getFromStage()).isEqualTo(OpportunityStage.PROSPECTING);
		assertThat(transition.getValue().getToStage()).isEqualTo(OpportunityStage.NEGOTIATION);
	}

	@Test
	@DisplayName("Arbitrary stage jumps are allowed in the flat kanban model")
	void arbitraryStageJumpsAreAllowedInTheFlatKanbanModel() {
		final OpportunityId id = OpportunityId.of(UUID.randomUUID());
		when(opportunityRepositoryPort.findById(id)).thenReturn(Optional.of(opportunity(id, OpportunityStage.PROSPECTING)));
		when(opportunityRepositoryPort.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(stageTransitionRepositoryPort.save(any(StageTransition.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final OpportunityView view = service.changeStage(new ChangeOpportunityStageCommand(id, OpportunityStage.CLOSED));

		assertThat(view.stage()).isEqualTo(OpportunityStage.CLOSED);
	}

	@Test
	@DisplayName("An opportunity in a terminal stage rejects further stage changes")
	void terminalStagesRejectFurtherStageChanges() {
		final OpportunityId id = OpportunityId.of(UUID.randomUUID());
		when(opportunityRepositoryPort.findById(id)).thenReturn(Optional.of(opportunity(id, OpportunityStage.CLOSED)));

		assertThatThrownBy(() -> service.changeStage(new ChangeOpportunityStageCommand(id, OpportunityStage.NEGOTIATION)))
				.isInstanceOf(BusinessRuleException.class);
		verify(opportunityRepositoryPort, never()).save(any());
		verify(stageTransitionRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("An opportunity in a terminal stage rejects field updates")
	void terminalStagesRejectFieldUpdates() {
		final OpportunityId id = OpportunityId.of(UUID.randomUUID());
		when(opportunityRepositoryPort.findById(id)).thenReturn(Optional.of(opportunity(id, OpportunityStage.LOST)));

		final UpdateOpportunityCommand command = UpdateOpportunityCommand.builder().opportunityId(id).owner(UUID.randomUUID()).build();

		assertThatThrownBy(() -> service.update(command)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Field updates are allowed in any non-terminal stage and keep unspecified fields unchanged")
	void fieldUpdatesAreAllowedAtAnyNonTerminalStageAndKeepUnspecifiedFieldsUnchanged() {
		final OpportunityId id = OpportunityId.of(UUID.randomUUID());
		final Opportunity existing = opportunity(id, OpportunityStage.PROPOSAL);
		when(opportunityRepositoryPort.findById(id)).thenReturn(Optional.of(existing));
		when(opportunityRepositoryPort.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final UpdateOpportunityCommand command = UpdateOpportunityCommand.builder().opportunityId(id).probability(90).build();

		final OpportunityView view = service.update(command);

		assertThat(view.probability()).isEqualTo(90);
		assertThat(view.estimatedValue()).isEqualTo(existing.getEstimatedValue());
		assertThat(view.owner()).isEqualTo(existing.getOwner());
	}

	@Test
	@DisplayName("Changing the stage of an opportunity that does not exist throws not-found")
	void changingStageOfAnUnknownOpportunityThrows() {
		final OpportunityId id = OpportunityId.of(UUID.randomUUID());
		when(opportunityRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.changeStage(new ChangeOpportunityStageCommand(id, OpportunityStage.PROPOSAL)))
				.isInstanceOf(OpportunityNotFoundException.class);
	}

	private Opportunity opportunity(final OpportunityId id, final OpportunityStage stage) {
		return Opportunity.of(id, UUID.randomUUID(), BigDecimal.valueOf(1000), 20, LocalDate.now().plusDays(10),
				UUID.randomUUID(), stage);
	}
}
