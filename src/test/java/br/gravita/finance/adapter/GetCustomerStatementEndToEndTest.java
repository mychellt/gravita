package br.gravita.finance.adapter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Renegotiation;
import br.gravita.core.domain.finance.RenegotiationId;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.RenegotiationRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class GetCustomerStatementEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Autowired
	private SettlementRepositoryPort settlementRepositoryPort;

	@Autowired
	private RenegotiationRepositoryPort renegotiationRepositoryPort;

	private final UUID customerId = UUID.randomUUID();
	private final UUID otherCustomerId = UUID.randomUUID();
	private Receivable renegotiated;
	private Receivable replacement;

	@BeforeEach
	void seed() {
		Receivable settled = save(customerId, "100.00", "2026-01-10", ReceivableStatus.SETTLED);
		save(settlementOf(settled, "100.00", "2026-01-09T10:00:00Z"));
		renegotiated = save(customerId, "200.00", "2026-02-10", ReceivableStatus.RENEGOTIATED);
		replacement = save(customerId, "210.00", "2026-04-10", ReceivableStatus.PARTIALLY_SETTLED);
		save(settlementOf(replacement, "60.00", "2026-04-01T10:00:00Z"));
		save(customerId, "30.00", "2026-05-10", ReceivableStatus.OPEN);
		save(customerId, "999.00", "2026-05-10", ReceivableStatus.CANCELLED);
		renegotiationRepositoryPort.save(Renegotiation.create(RenegotiationId.of(UUID.randomUUID()), customerId,
				List.of(renegotiated.getId()), List.of(replacement.getId()), Instant.parse("2026-03-01T10:00:00Z")));
		save(otherCustomerId, "5000.00", "2026-01-10", ReceivableStatus.OPEN);
	}

	private Receivable save(UUID customer, String amount, String dueDate, ReceivableStatus status) {
		return receivableRepositoryPort.save(Receivable.of(ReceivableId.of(UUID.randomUUID()), customer,
				ReceivableOrigin.MANUAL, new BigDecimal(amount), LocalDate.parse(dueDate), null, status, null, null));
	}

	private Settlement settlementOf(Receivable receivable, String amount, String at) {
		return Settlement.manual(SettlementId.of(UUID.randomUUID()), receivable.getId(), new BigDecimal(amount), null,
				null, null, null, Instant.parse(at));
	}

	private void save(Settlement settlement) {
		settlementRepositoryPort.save(settlement);
	}

	@Test
	@DisplayName("Returns the customer's titles, settlements and renegotiations together with the open balance")
	void returnsTheCustomersTitlesSettlementsAndRenegotiationsWithTheOpenBalance() throws Exception {
		mockMvc.perform(get("/api/finance/customers/{id}/statement", customerId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.customerId").value(customerId.toString()))
				.andExpect(jsonPath("$.titles.length()").value(5))
				.andExpect(jsonPath("$.titles[0].dueDate").value("2026-01-10"))
				.andExpect(jsonPath("$.titles[1].status").value("RENEGOTIATED"))
				.andExpect(jsonPath("$.settlements.length()").value(2))
				.andExpect(jsonPath("$.settlements[0].amount").value(100.0))
				.andExpect(jsonPath("$.settlements[1].amount").value(60.0))
				.andExpect(jsonPath("$.renegotiations.length()").value(1))
				.andExpect(jsonPath("$.renegotiations[0].originalReceivableIds[0]")
						.value(renegotiated.getId().value().toString()))
				.andExpect(jsonPath("$.openBalance").value(180.0));
	}

	@Test
	@DisplayName("Narrows the lists to the requested period without changing the open balance")
	void thePeriodNarrowsTheListsButNotTheOpenBalance() throws Exception {
		mockMvc.perform(get("/api/finance/customers/{id}/statement", customerId).param("from", "2026-02-01")
				.param("to", "2026-03-31")).andExpect(status().isOk())
				.andExpect(jsonPath("$.titles.length()").value(1))
				.andExpect(jsonPath("$.settlements.length()").value(0))
				.andExpect(jsonPath("$.renegotiations.length()").value(1))
				.andExpect(jsonPath("$.openBalance").value(180.0));
	}

	@Test
	@DisplayName("Returns an empty statement for a customer without titles")
	void aCustomerWithoutTitlesGetsAnEmptyStatement() throws Exception {
		mockMvc.perform(get("/api/finance/customers/{id}/statement", UUID.randomUUID())).andExpect(status().isOk())
				.andExpect(jsonPath("$.titles.length()").value(0)).andExpect(jsonPath("$.openBalance").value(0.0));
	}

	@Test
	@DisplayName("Responds 400 Bad Request when the period ends before it starts")
	void aPeriodThatEndsBeforeItStartsIsA400() throws Exception {
		mockMvc.perform(get("/api/finance/customers/{id}/statement", customerId).param("from", "2026-03-01")
				.param("to", "2026-02-01")).andExpect(status().isBadRequest());
	}
}
