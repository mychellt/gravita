package br.gravita.finance.adapter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class GetAgingListEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Autowired
	private SettlementRepositoryPort settlementRepositoryPort;

	private final LocalDate today = LocalDate.now();
	private final UUID customerA = UUID.randomUUID();
	private final UUID customerB = UUID.randomUUID();

	@BeforeEach
	void seed() {
		savedReceivable(customerA, "10.00", today.minusDays(3), ReceivableStatus.OPEN);
		savedReceivable(customerA, "20.00", today.minusDays(31), ReceivableStatus.OPEN);
		Receivable partial = savedReceivable(customerA, "100.00", today.minusDays(75),
				ReceivableStatus.PARTIALLY_SETTLED);
		settlementRepositoryPort.save(Settlement.manual(SettlementId.of(UUID.randomUUID()), partial.getId(),
				new BigDecimal("40.00"), null, null, null, null, Instant.now()));
		savedReceivable(customerB, "1000.00", today.minusDays(200), ReceivableStatus.OPEN);
		// Not part of the aging: not due yet, settled, renegotiated, cancelled.
		savedReceivable(customerA, "7.00", today.plusDays(1), ReceivableStatus.OPEN);
		savedReceivable(customerA, "7.00", today.minusDays(10), ReceivableStatus.SETTLED);
		savedReceivable(customerA, "7.00", today.minusDays(10), ReceivableStatus.RENEGOTIATED);
		savedReceivable(customerA, "7.00", today.minusDays(10), ReceivableStatus.CANCELLED);
	}

	private Receivable savedReceivable(UUID customerId, String amount, LocalDate dueDate, ReceivableStatus status) {
		return receivableRepositoryPort.save(Receivable.of(ReceivableId.of(UUID.randomUUID()), customerId,
				ReceivableOrigin.MANUAL, new BigDecimal(amount), dueDate, null, status, null, null));
	}

	private ResultActions aging(String... params) throws Exception {
		var request = get("/api/finance/receivables/aging");
		for (int i = 0; i < params.length; i += 2) {
			request.param(params[i], params[i + 1]);
		}
		return mockMvc.perform(request).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Buckets the outstanding titles of every customer into the aging ranges")
	void bucketsEveryCustomersOutstandingTitles() throws Exception {
		aging().andExpect(jsonPath("$.asOfDate").value(today.toString()))
				.andExpect(jsonPath("$.buckets.length()").value(4))
				.andExpect(jsonPath("$.buckets[0].range").value("UP_TO_30"))
				.andExpect(jsonPath("$.buckets[0].fromDays").value(0))
				.andExpect(jsonPath("$.buckets[0].toDays").value(30))
				.andExpect(jsonPath("$.buckets[0].total").value(10.0))
				.andExpect(jsonPath("$.buckets[1].total").value(20.0))
				.andExpect(jsonPath("$.buckets[2].total").value(60.0))
				.andExpect(jsonPath("$.buckets[3].fromDays").value(91))
				.andExpect(jsonPath("$.buckets[3].toDays").doesNotExist())
				.andExpect(jsonPath("$.buckets[3].total").value(1000.0))
				.andExpect(jsonPath("$.total").value(1090.0))
				.andExpect(jsonPath("$.titleCount").value(4));
	}

	@Test
	@DisplayName("Returns only the given customer's titles when filtering the aging list by customer")
	void filtersByCustomer() throws Exception {
		aging("customerId", customerB.toString()).andExpect(jsonPath("$.buckets[3].total").value(1000.0))
				.andExpect(jsonPath("$.total").value(1000.0)).andExpect(jsonPath("$.titleCount").value(1));
		aging("customerId", customerA.toString()).andExpect(jsonPath("$.total").value(90.0));
	}

	@Test
	@DisplayName("Counts the days overdue up to the as-of date given in the request")
	void countsDaysOverdueUpToTheGivenDate() throws Exception {
		aging("asOfDate", today.minusDays(250).toString()).andExpect(jsonPath("$.asOfDate")
				.value(today.minusDays(250).toString())).andExpect(jsonPath("$.total").value(0.0));
		aging("asOfDate", today.plusDays(60).toString()).andExpect(jsonPath("$.titleCount").value(5));
	}

	@Test
	@DisplayName("Returns an empty aging report when a cost center filter is supplied")
	void aCostCenterYieldsAnEmptyReport() throws Exception {
		aging("costCenterId", UUID.randomUUID().toString()).andExpect(jsonPath("$.titleCount").value(0))
				.andExpect(jsonPath("$.total").value(0.0));
	}

	@Test
	@DisplayName("Responds 400 Bad Request when the as-of date is malformed")
	void aMalformedDateIsA400() throws Exception {
		mockMvc.perform(get("/api/finance/receivables/aging").param("asOfDate", "yesterday"))
				.andExpect(status().isBadRequest());
	}
}
