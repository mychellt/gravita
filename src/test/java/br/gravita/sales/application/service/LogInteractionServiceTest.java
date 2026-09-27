package br.gravita.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.sales.Interaction;
import br.gravita.core.domain.sales.InteractionChannel;
import br.gravita.core.domain.sales.OpportunityId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.InteractionView;
import br.gravita.core.ports.inbound.sales.LogInteractionCommand;
import br.gravita.core.ports.outbound.persistence.sales.InteractionRepositoryPort;
import br.gravita.core.usercases.sales.LogInteractionService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogInteractionServiceTest {

	@Mock
	private InteractionRepositoryPort interactionRepositoryPort;

	@InjectMocks
	private LogInteractionService service;

	@Test
	void logsAnInteractionLinkedToAnOpportunity() {
		when(interactionRepositoryPort.save(any(Interaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		OpportunityId opportunityId = OpportunityId.of(UUID.randomUUID());
		LogInteractionCommand command = LogInteractionCommand.builder()
				.opportunityId(opportunityId)
				.channel(InteractionChannel.CALL)
				.summary("Discussed pricing")
				.timestamp(Instant.now())
				.build();

		InteractionView view = service.execute(command);

		assertThat(view.opportunityId()).isEqualTo(opportunityId.value());
		assertThat(view.customerId()).isNull();
		assertThat(view.channel()).isEqualTo(InteractionChannel.CALL);
	}

	@Test
	void logsAnInteractionLinkedOnlyToACustomer() {
		when(interactionRepositoryPort.save(any(Interaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UUID customerId = UUID.randomUUID();
		LogInteractionCommand command = LogInteractionCommand.builder()
				.customerId(customerId)
				.channel(InteractionChannel.WHATSAPP)
				.summary("Sent a follow-up message")
				.timestamp(Instant.now())
				.build();

		InteractionView view = service.execute(command);

		assertThat(view.opportunityId()).isNull();
		assertThat(view.customerId()).isEqualTo(customerId);
	}

	@Test
	void rejectsAnInteractionWithNeitherOpportunityNorCustomer() {
		LogInteractionCommand command = LogInteractionCommand.builder()
				.channel(InteractionChannel.EMAIL)
				.summary("No target")
				.timestamp(Instant.now())
				.build();

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class);
	}
}
