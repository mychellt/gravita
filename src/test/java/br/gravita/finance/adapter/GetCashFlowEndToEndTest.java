package br.gravita.finance.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.finance.CashFlowFilter;
import br.gravita.core.domain.finance.CostCenterShare;
import br.gravita.core.domain.finance.LedgerScope;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableOrigin;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.domain.finance.SettlementMethod;
import br.gravita.core.ports.outbound.finance.NegativeBalanceProjectionAlert;
import br.gravita.core.ports.outbound.finance.NotifyNegativeBalanceProjectionPort;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class GetCashFlowEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Autowired
	private PayableRepositoryPort payableRepositoryPort;

	@Autowired
	private SettlementRepositoryPort settlementRepositoryPort;

	@MockitoBean
	private NotifyNegativeBalanceProjectionPort notifyPort;

	private final UUID companyA = UUID.randomUUID();
	private final UUID companyB = UUID.randomUUID();
	private final UUID branchA1 = UUID.randomUUID();
	private final UUID branchA2 = UUID.randomUUID();
	private final UUID bankA = UUID.randomUUID();
	private final UUID bankB = UUID.randomUUID();
	private final UUID costCenter = UUID.randomUUID();
	private final LocalDate today = LocalDate.now();
	private final LocalDate dueDay = today.plusDays(3);

	@BeforeEach
	void seed() {
		// Company A / branch A1 / bank A: 100 to receive, 30 already received (+2 interest), 80 to pay (all cost center), 20 paid.
		Receivable openA = savedReceivable("100.00", dueDay, ReceivableStatus.OPEN, new LedgerScope(companyA, branchA1, bankA));
		Receivable paidA = savedReceivable("30.00", today, ReceivableStatus.SETTLED, new LedgerScope(companyA, branchA1, bankA));
		settlementRepositoryPort.save(Settlement.manual(SettlementId.of(UUID.randomUUID()), paidA.getId(),
				new BigDecimal("30.00"), new BigDecimal("2.00"), null, null, null, Instant.now()));
		savedPayable("80.00", dueDay, PayableStatus.OPEN, new LedgerScope(companyA, branchA1, bankA),
				List.of(new CostCenterShare(costCenter, new BigDecimal("100"))));
		Payable paidPayableA = savedPayable("20.00", today, PayableStatus.PAID, new LedgerScope(companyA, branchA1, bankA),
				List.of(new CostCenterShare(costCenter, new BigDecimal("50")),
						new CostCenterShare(UUID.randomUUID(), new BigDecimal("50"))));
		settlementRepositoryPort.save(Settlement.ofPayable(SettlementId.of(UUID.randomUUID()), paidPayableA.getId(),
				new BigDecimal("20.00"), null, null, null, null, SettlementMethod.MANUAL, Instant.now()));

		// Company A / branch A2 / bank B and Company B: only open titles, of different amounts.
		savedReceivable("1000.00", dueDay, ReceivableStatus.OPEN, new LedgerScope(companyA, branchA2, bankB));
		savedReceivable("10000.00", dueDay, ReceivableStatus.OPEN, new LedgerScope(companyB, null, null));
		savedPayable("5.00", dueDay, PayableStatus.APPROVED, new LedgerScope(companyB, null, null), List.of());
		// Not outstanding, so never projected.
		savedReceivable("777.00", dueDay, ReceivableStatus.CANCELLED, new LedgerScope(companyA, branchA1, bankA));
		savedPayable("777.00", dueDay, PayableStatus.CANCELLED, new LedgerScope(companyA, branchA1, bankA), List.of());
	}

	private Receivable savedReceivable(String amount, LocalDate dueDate, ReceivableStatus status, LedgerScope scope) {
		return receivableRepositoryPort.save(Receivable.of(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(),
				ReceivableOrigin.MANUAL, new BigDecimal(amount), dueDate, null, status, null, null, scope));
	}

	private Payable savedPayable(String amount, LocalDate dueDate, PayableStatus status, LedgerScope scope,
			List<CostCenterShare> split) {
		return payableRepositoryPort.save(Payable.of(PayableId.of(UUID.randomUUID()), null, PayableOrigin.MANUAL,
				new BigDecimal(amount), dueDate, split, status, null, null, null, scope));
	}

	private ResultActions cashFlow(String... params) throws Exception {
		var request = get("/api/finance/cash-flow").param("granularity", "DAILY")
				.param("from", today.toString()).param("to", today.plusDays(5).toString());
		for (int i = 0; i < params.length; i += 2) {
			request.param(params[i], params[i + 1]);
		}
		return mockMvc.perform(request).andExpect(status().isOk());
	}

	private static String at(int day, String field) {
		return "$.buckets[" + day + "]." + field;
	}

	@Test
	@DisplayName("Combines everything realized and open when no filter is given")
	void unfilteredCombinesEverythingRealizedAndOpen() throws Exception {
		cashFlow().andExpect(jsonPath("$.buckets.length()").value(6))
				.andExpect(jsonPath(at(0, "realizedInflow")).value(32.0))
				.andExpect(jsonPath(at(0, "realizedOutflow")).value(20.0))
				.andExpect(jsonPath(at(3, "projectedInflow")).value(11100.0))
				.andExpect(jsonPath(at(3, "projectedOutflow")).value(85.0))
				.andExpect(jsonPath("$.closingBalance").value(11027.0));
		verify(notifyPort, never()).notify(any());
	}

	@Test
	@DisplayName("Returns only the given company's movements when filtering by company")
	void filtersByCompany() throws Exception {
		cashFlow("companyId", companyB.toString()).andExpect(jsonPath(at(0, "realizedInflow")).value(0.0))
				.andExpect(jsonPath(at(3, "projectedInflow")).value(10000.0))
				.andExpect(jsonPath(at(3, "projectedOutflow")).value(5.0));
	}

	@Test
	@DisplayName("Returns only the given branch's movements when filtering by branch")
	void filtersByBranch() throws Exception {
		cashFlow("branchId", branchA2.toString()).andExpect(jsonPath(at(0, "realizedInflow")).value(0.0))
				.andExpect(jsonPath(at(3, "projectedInflow")).value(1000.0))
				.andExpect(jsonPath(at(3, "projectedOutflow")).value(0.0));
	}

	@Test
	@DisplayName("Applies the bank account filter to both realized and open entries")
	void filtersByBankAccountOnBothTheRealizedAndTheOpenSide() throws Exception {
		cashFlow("bankAccountId", bankA.toString()).andExpect(jsonPath(at(0, "realizedInflow")).value(32.0))
				.andExpect(jsonPath(at(0, "realizedOutflow")).value(20.0))
				.andExpect(jsonPath(at(3, "projectedInflow")).value(100.0))
				.andExpect(jsonPath(at(3, "projectedOutflow")).value(80.0));
	}

	@Test
	@DisplayName("Keeps only payables, at their cost center share, when filtering by cost center")
	void filtersByCostCenterKeepingOnlyPayablesAndTheirShare() throws Exception {
		cashFlow("costCenterId", costCenter.toString()).andExpect(jsonPath(at(0, "realizedInflow")).value(0.0))
				.andExpect(jsonPath(at(0, "realizedOutflow")).value(10.0))
				.andExpect(jsonPath(at(3, "projectedInflow")).value(0.0))
				.andExpect(jsonPath(at(3, "projectedOutflow")).value(80.0));
	}

	@Test
	@DisplayName("Applies several filters together")
	void combinesFilters() throws Exception {
		cashFlow("companyId", companyA.toString(), "branchId", branchA1.toString(), "bankAccountId", bankA.toString())
				.andExpect(jsonPath(at(3, "projectedInflow")).value(100.0))
				.andExpect(jsonPath(at(3, "projectedOutflow")).value(80.0));
		cashFlow("companyId", companyB.toString(), "bankAccountId", bankA.toString())
				.andExpect(jsonPath("$.closingBalance").value(0.0));
	}

	@Test
	@DisplayName("Raises the negative balance alert carrying the filter that was applied")
	void aNegativeProjectionTriggersTheAlertWithTheFilterThatWasApplied() throws Exception {
		cashFlow("costCenterId", costCenter.toString(), "openingBalance", "0")
				.andExpect(jsonPath("$.closingBalance").value(-90.0));

		ArgumentCaptor<NegativeBalanceProjectionAlert> alert = ArgumentCaptor
				.forClass(NegativeBalanceProjectionAlert.class);
		verify(notifyPort).notify(alert.capture());
		assertThat(alert.getValue().filter()).isEqualTo(new CashFlowFilter(null, null, null, costCenter));
		assertThat(alert.getValue().firstNegativePeriodStart()).isEqualTo(today);
		assertThat(alert.getValue().lowestBalance()).isEqualByComparingTo("-90.00");
	}

	@Test
	@DisplayName("Responds 400 Bad Request when the range ends before it starts")
	void aRangeThatEndsBeforeItStartsIsA400() throws Exception {
		mockMvc.perform(get("/api/finance/cash-flow").param("from", today.toString()).param("to",
				today.minusDays(1).toString())).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Responds 400 Bad Request when the granularity is unknown")
	void anUnknownGranularityIsA400() throws Exception {
		mockMvc.perform(get("/api/finance/cash-flow").param("granularity", "YEARLY"))
				.andExpect(status().isBadRequest());
	}
}
