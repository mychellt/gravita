package br.gravita.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.sales.FollowUpRule;
import br.gravita.core.domain.sales.FollowUpRuleId;
import br.gravita.core.domain.sales.FollowUpRuleNotFoundException;
import br.gravita.core.domain.sales.FollowUpTarget;
import br.gravita.core.ports.inbound.sales.CreateFollowUpRuleCommand;
import br.gravita.core.ports.inbound.sales.FollowUpRuleView;
import br.gravita.core.ports.inbound.sales.UpdateFollowUpRuleCommand;
import br.gravita.core.ports.outbound.persistence.sales.FollowUpRuleRepositoryPort;
import br.gravita.core.usercases.sales.ManageFollowUpRuleService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManageFollowUpRuleServiceTest {

	@Mock
	private FollowUpRuleRepositoryPort followUpRuleRepositoryPort;

	@InjectMocks
	private ManageFollowUpRuleService service;

	@Test
	void createsANewFollowUpRule() {
		when(followUpRuleRepositoryPort.save(any(FollowUpRule.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		CreateFollowUpRuleCommand command = CreateFollowUpRuleCommand.builder()
				.daysWithoutContact(7)
				.target(FollowUpTarget.CUSTOMER)
				.notifyOwner(true)
				.active(true)
				.build();

		FollowUpRuleView view = service.create(command);

		assertThat(view.daysWithoutContact()).isEqualTo(7);
		assertThat(view.target()).isEqualTo(FollowUpTarget.CUSTOMER);
		assertThat(view.notifyOwner()).isTrue();
		assertThat(view.active()).isTrue();
	}

	@Test
	void rejectsNonPositiveDaysWithoutContactOnCreate() {
		CreateFollowUpRuleCommand command = CreateFollowUpRuleCommand.builder()
				.daysWithoutContact(0)
				.target(FollowUpTarget.CUSTOMER)
				.notifyOwner(true)
				.active(true)
				.build();

		assertThatThrownBy(() -> service.create(command)).isInstanceOf(RuntimeException.class);
	}

	@Test
	void partialUpdateKeepsUnspecifiedFieldsUnchanged() {
		FollowUpRuleId id = FollowUpRuleId.of(UUID.randomUUID());
		FollowUpRule existing = FollowUpRule.of(id, 7, FollowUpTarget.CUSTOMER, true, true);
		when(followUpRuleRepositoryPort.findById(id)).thenReturn(Optional.of(existing));
		when(followUpRuleRepositoryPort.save(any(FollowUpRule.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		UpdateFollowUpRuleCommand command = UpdateFollowUpRuleCommand.builder().ruleId(id).active(false).build();

		FollowUpRuleView view = service.update(command);

		assertThat(view.active()).isFalse();
		assertThat(view.daysWithoutContact()).isEqualTo(7);
		assertThat(view.target()).isEqualTo(FollowUpTarget.CUSTOMER);
	}

	@Test
	void updatingAnUnknownRuleThrows() {
		FollowUpRuleId id = FollowUpRuleId.of(UUID.randomUUID());
		when(followUpRuleRepositoryPort.findById(id)).thenReturn(Optional.empty());

		UpdateFollowUpRuleCommand command = UpdateFollowUpRuleCommand.builder().ruleId(id).active(false).build();

		assertThatThrownBy(() -> service.update(command)).isInstanceOf(FollowUpRuleNotFoundException.class);
	}

	@Test
	void deletingARuleRemovesItFromTheRepository() {
		FollowUpRuleId id = FollowUpRuleId.of(UUID.randomUUID());
		FollowUpRule existing = FollowUpRule.of(id, 7, FollowUpTarget.CUSTOMER, true, true);
		when(followUpRuleRepositoryPort.findById(id)).thenReturn(Optional.of(existing));

		service.delete(id);

		verify(followUpRuleRepositoryPort).deleteById(id);
	}

	@Test
	void deletingAnUnknownRuleThrows() {
		FollowUpRuleId id = FollowUpRuleId.of(UUID.randomUUID());
		when(followUpRuleRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(id)).isInstanceOf(FollowUpRuleNotFoundException.class);
	}
}
