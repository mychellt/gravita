package br.gravita.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.sales.FollowUpRule;
import br.gravita.core.domain.sales.FollowUpRuleId;
import br.gravita.core.domain.sales.FollowUpTarget;
import br.gravita.core.ports.inbound.sales.FollowUpRuleView;
import br.gravita.core.ports.outbound.persistence.sales.FollowUpRuleRepositoryPort;
import br.gravita.core.usercases.sales.ListFollowUpRulesService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListFollowUpRulesServiceTest {

	@Mock
	private FollowUpRuleRepositoryPort followUpRuleRepositoryPort;

	@InjectMocks
	private ListFollowUpRulesService service;

	@Test
	@DisplayName("Lists both active and inactive follow-up rules")
	void listsBothActiveAndInactiveRules() {
		final FollowUpRule active =
				FollowUpRule.of(FollowUpRuleId.of(UUID.randomUUID()), 7, FollowUpTarget.CUSTOMER, true, true);
		final FollowUpRule inactive =
				FollowUpRule.of(FollowUpRuleId.of(UUID.randomUUID()), 14, FollowUpTarget.OPPORTUNITY, false, false);
		when(followUpRuleRepositoryPort.findAll()).thenReturn(List.of(active, inactive));

		final List<FollowUpRuleView> views = service.execute();

		assertThat(views).hasSize(2);
		assertThat(views).extracting(FollowUpRuleView::active).containsExactly(true, false);
	}
}
