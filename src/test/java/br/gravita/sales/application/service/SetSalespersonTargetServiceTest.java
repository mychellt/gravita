package br.gravita.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.sales.SalespersonTarget;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.SetSalespersonTargetCommand;
import br.gravita.core.ports.outbound.persistence.sales.SalespersonTargetRepositoryPort;
import br.gravita.core.usercases.sales.SetSalespersonTargetService;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SetSalespersonTargetServiceTest {

	@Mock
	private SalespersonTargetRepositoryPort salespersonTargetRepositoryPort;

	@InjectMocks
	private SetSalespersonTargetService service;

	@Test
	@DisplayName("Persists the target for the given salesperson and month")
	void persistsTheTargetForTheGivenSalespersonAndMonth() {
		UUID salesperson = UUID.randomUUID();
		YearMonth month = YearMonth.of(2026, 1);
		SetSalespersonTargetCommand command =
				new SetSalespersonTargetCommand(salesperson, month, new BigDecimal("15000.00"), 30);
		when(salespersonTargetRepositoryPort.save(any(SalespersonTarget.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(command);

		ArgumentCaptor<SalespersonTarget> captor = ArgumentCaptor.forClass(SalespersonTarget.class);
		verify(salespersonTargetRepositoryPort).save(captor.capture());
		SalespersonTarget saved = captor.getValue();
		assertThat(saved.salespersonId()).isEqualTo(salesperson);
		assertThat(saved.month()).isEqualTo(month);
		assertThat(saved.valueTarget()).isEqualByComparingTo("15000.00");
		assertThat(saved.orderCountTarget()).isEqualTo(30);
	}

	@Test
	@DisplayName("Rejects a target with a negative value")
	void rejectsNegativeValueTarget() {
		SetSalespersonTargetCommand command = new SetSalespersonTargetCommand(UUID.randomUUID(),
				YearMonth.of(2026, 1), new BigDecimal("-1"), 10);

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects a target with a negative order count")
	void rejectsNegativeOrderCountTarget() {
		SetSalespersonTargetCommand command = new SetSalespersonTargetCommand(UUID.randomUUID(),
				YearMonth.of(2026, 1), new BigDecimal("100"), -5);

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class);
	}
}
