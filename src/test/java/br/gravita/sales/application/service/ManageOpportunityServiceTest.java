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
	void createsAnOpportunityInTheProspectingStage() {
		when(opportunityRepositoryPort.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateOpportunityCommand command = new CreateOpportunityCommand(UUID.randomUUID(), BigDecimal.TEN, 50,
				LocalDate.now().plusDays(30), UUID.randomUUID());

		OpportunityView view = service.create(command);

		assertThat(view.stage()).isEqualTo(OpportunityStage.PROSPECTING);
		verify(stageTransitionRepositoryPort, never()).save(any());
	}

	@Test
	void changingStageAppendsExactlyOneStageTransition() {
		OpportunityId id = OpportunityId.of(UUID.randomUUID());
		when(opportunityRepositoryPort.findById(id)).thenReturn(Optional.of(opportunity(id, OpportunityStage.PROSPECTING)));
		when(opportunityRepositoryPort.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(stageTransitionRepositoryPort.save(any(StageTransition.class))).thenAnswer(invocation -> invocation.getArgument(0));

		OpportunityView view = service.changeStage(
				new ChangeOpportunityStageCommand(id, OpportunityStage.NEGOTIATION));

		assertThat(view.stage()).isEqualTo(OpportunityStage.NEGOTIATION);
		ArgumentCaptor<StageTransition> transition = ArgumentCaptor.forClass(StageTransition.class);
		verify(stageTransitionRepositoryPort).save(transition.capture());
		assertThat(transition.getValue().getFromStage()).isEqualTo(OpportunityStage.PROSPECTING);
		assertThat(transition.getValue().getToStage()).isEqualTo(OpportunityStage.NEGOTIATION);
	}

	@Test
	void arbitraryStageJumpsAreAllowedInTheFlatKanbanModel() {
		OpportunityId id = OpportunityId.of(UUID.randomUUID());
		when(opportunityRepositoryPort.findById(id)).thenReturn(Optional.of(opportunity(id, OpportunityStage.PROSPECTING)));
		when(opportunityRepositoryPort.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(stageTransitionRepositoryPort.save(any(StageTransition.class))).thenAnswer(invocation -> invocation.getArgument(0));

		OpportunityView view = service.changeStage(new ChangeOpportunityStageCommand(id, OpportunityStage.CLOSED));

		assertThat(view.stage()).isEqualTo(OpportunityStage.CLOSED);
	}

	@Test
	void terminalStagesRejectFurtherStageChanges() {
		OpportunityId id = OpportunityId.of(UUID.randomUUID());
		when(opportunityRepositoryPort.findById(id)).thenReturn(Optional.of(opportunity(id, OpportunityStage.CLOSED)));

		assertThatThrownBy(() -> service.changeStage(new ChangeOpportunityStageCommand(id, OpportunityStage.NEGOTIATION)))
				.isInstanceOf(BusinessRuleException.class);
		verify(opportunityRepositoryPort, never()).save(any());
		verify(stageTransitionRepositoryPort, never()).save(any());
	}

	@Test
	void terminalStagesRejectFieldUpdates() {
		OpportunityId id = OpportunityId.of(UUID.randomUUID());
		when(opportunityRepositoryPort.findById(id)).thenReturn(Optional.of(opportunity(id, OpportunityStage.LOST)));

		UpdateOpportunityCommand command = UpdateOpportunityCommand.builder().opportunityId(id).owner(UUID.randomUUID()).build();

		assertThatThrownBy(() -> service.update(command)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void fieldUpdatesAreAllowedAtAnyNonTerminalStageAndKeepUnspecifiedFieldsUnchanged() {
		OpportunityId id = OpportunityId.of(UUID.randomUUID());
		Opportunity existing = opportunity(id, OpportunityStage.PROPOSAL);
		when(opportunityRepositoryPort.findById(id)).thenReturn(Optional.of(existing));
		when(opportunityRepositoryPort.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UpdateOpportunityCommand command = UpdateOpportunityCommand.builder().opportunityId(id).probability(90).build();

		OpportunityView view = service.update(command);

		assertThat(view.probability()).isEqualTo(90);
		assertThat(view.estimatedValue()).isEqualTo(existing.getEstimatedValue());
		assertThat(view.owner()).isEqualTo(existing.getOwner());
	}

	@Test
	void changingStageOfAnUnknownOpportunityThrows() {
		OpportunityId id = OpportunityId.of(UUID.randomUUID());
		when(opportunityRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.changeStage(new ChangeOpportunityStageCommand(id, OpportunityStage.PROPOSAL)))
				.isInstanceOf(OpportunityNotFoundException.class);
	}

	private Opportunity opportunity(OpportunityId id, OpportunityStage stage) {
		return Opportunity.of(id, UUID.randomUUID(), BigDecimal.valueOf(1000), 20, LocalDate.now().plusDays(10),
				UUID.randomUUID(), stage);
	}
}
