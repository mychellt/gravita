package br.gravita.system.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.gravita.core.ports.inbound.system.PlatformPaymentView;
import br.gravita.core.ports.outbound.persistence.system.PlatformCustomerQueryPort;
import br.gravita.core.usercases.system.ListPlatformCustomerPaymentsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListPlatformCustomerPaymentsServiceTest {

	private final PlatformCustomerQueryPort queryPort = mock(PlatformCustomerQueryPort.class);
	private final ListPlatformCustomerPaymentsService service = new ListPlatformCustomerPaymentsService(queryPort);

	@Test
	@DisplayName("lists the payments of the company, latest first")
	void latestFirst() {
		final UUID companyId = UUID.randomUUID();
		final PlatformPaymentView old = new PlatformPaymentView(UUID.randomUUID(), BigDecimal.TEN, LocalDate.of(2026, 1, 10));
		final PlatformPaymentView recent = new PlatformPaymentView(UUID.randomUUID(), BigDecimal.TEN, LocalDate.of(2026, 9, 10));
		when(queryPort.findPaymentsByCompanyId(companyId)).thenReturn(List.of(old, recent));

		assertThat(service.execute(companyId)).containsExactly(recent, old);
	}
}
