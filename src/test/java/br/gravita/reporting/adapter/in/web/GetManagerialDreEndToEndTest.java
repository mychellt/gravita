package br.gravita.reporting.adapter.in.web;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.finance.CostCenterShare;
import br.gravita.core.domain.finance.LedgerScope;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableOrigin;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseTomador;
import br.gravita.core.domain.tax.PlaceOfProvision;
import br.gravita.core.domain.tax.ServiceCode;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import br.gravita.core.usercases.system.CheckPermissionUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the real controller, service and read-model adapters over the real repositories; only session and permission
 * are stubbed. May 2016 is long past and has no sales, so revenue and CMV are zero and other tests cannot collide.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class GetManagerialDreEndToEndTest {

	private static final ZoneId ZONE = ZoneId.systemDefault();
	private static final String MUNICIPALITY = "3304557";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PayableRepositoryPort payableRepositoryPort;

	@Autowired
	private NfseRepositoryPort nfseRepositoryPort;

	@MockitoBean
	private SessionStorePort sessionStorePort;

	@MockitoBean
	private CheckPermissionUseCase checkPermissionUseCase;

	private final CompanyId company = CompanyId.of(UUID.randomUUID());
	private final UUID rent = UUID.randomUUID();
	private final UUID marketing = UUID.randomUUID();
	private long rpsNumber = 800_000L;

	@BeforeEach
	void seed() {
		UserId caller = UserId.generate();
		when(sessionStorePort.resolve("valid-token")).thenReturn(Optional.of(caller));
		when(checkPermissionUseCase.execute(argThat(
				query -> query != null && query.userId().equals(caller) && query.module().equals("reporting")
						&& query.screen().equals("dre")))).thenReturn(true);

		// Deductions: the ISS of the NFSe authorized in the month.
		nfseRepositoryPort.save(nfse(at(5, 10, 9, 0), "42.50"));

		// Expenses due in the month: one split 60/40 between two cost centers, one charged to rent only, one to none.
		payable(PayableOrigin.MANUAL, PayableStatus.OPEN, "1000.00", LocalDate.of(2016, 5, 1),
				new CostCenterShare(rent, new BigDecimal("60")), new CostCenterShare(marketing, new BigDecimal("40")));
		payable(PayableOrigin.MANUAL, PayableStatus.PAID, "200.00", LocalDate.of(2016, 5, 31),
				new CostCenterShare(rent, new BigDecimal("100")));
		payable(PayableOrigin.MANUAL, PayableStatus.APPROVED, "50.00", LocalDate.of(2016, 5, 15));
		// Not expenses of the month: cancelled, a purchase of goods, another company's, or due outside the month.
		payable(PayableOrigin.MANUAL, PayableStatus.CANCELLED, "999.00", LocalDate.of(2016, 5, 10),
				new CostCenterShare(rent, new BigDecimal("100")));
		payable(PayableOrigin.PURCHASE_RECEIPT, PayableStatus.OPEN, "999.00", LocalDate.of(2016, 5, 10));
		payableRepositoryPort.save(Payable.of(PayableId.of(UUID.randomUUID()), null, PayableOrigin.MANUAL, new BigDecimal("999.00"),
				LocalDate.of(2016, 5, 10), List.of(new CostCenterShare(rent, new BigDecimal("100"))),
				PayableStatus.OPEN, null, null, null, new LedgerScope(UUID.randomUUID(), null, null)));
		payable(PayableOrigin.MANUAL, PayableStatus.OPEN, "999.00", LocalDate.of(2016, 4, 30),
				new CostCenterShare(rent, new BigDecimal("100")));
		payable(PayableOrigin.MANUAL, PayableStatus.OPEN, "999.00", LocalDate.of(2016, 6, 1),
				new CostCenterShare(rent, new BigDecimal("100")));
	}

	@Test
	void composesTheDreOfThePeriodWithTheExpensesByCostCenter() throws Exception {
		mockMvc.perform(get("/api/reports/dre").param("period", "2016-05").param("companyId", company.value().toString())
				.header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.period").value("2016-05"))
				.andExpect(jsonPath("$.costCenter").doesNotExist())
				.andExpect(jsonPath("$.grossRevenue").value(0))
				.andExpect(jsonPath("$.deductions").value(42.5))
				.andExpect(jsonPath("$.cmv").value(0))
				.andExpect(jsonPath("$.expensesByCostCenter.length()").value(3))
				.andExpect(jsonPath("$.expensesByCostCenter[0].costCenterId").value(rent.toString()))
				.andExpect(jsonPath("$.expensesByCostCenter[0].amount").value(800.0))
				.andExpect(jsonPath("$.expensesByCostCenter[1].costCenterId").value(marketing.toString()))
				.andExpect(jsonPath("$.expensesByCostCenter[1].amount").value(400.0))
				.andExpect(jsonPath("$.expensesByCostCenter[2].costCenterId").doesNotExist())
				.andExpect(jsonPath("$.expensesByCostCenter[2].amount").value(50.0))
				.andExpect(jsonPath("$.totalExpenses").value(1250.0))
				.andExpect(jsonPath("$.netResult").value(-1292.5));
	}

	@Test
	void narrowsTheExpensesToTheRequestedCostCenter() throws Exception {
		mockMvc.perform(get("/api/reports/dre").param("period", "2016-05").param("costCenter", marketing.toString())
				.param("companyId", company.value().toString()).header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.costCenter").value(marketing.toString()))
				.andExpect(jsonPath("$.deductions").value(42.5))
				.andExpect(jsonPath("$.expensesByCostCenter.length()").value(1))
				.andExpect(jsonPath("$.expensesByCostCenter[0].costCenterId").value(marketing.toString()))
				.andExpect(jsonPath("$.expensesByCostCenter[0].amount").value(400.0))
				.andExpect(jsonPath("$.totalExpenses").value(400.0))
				.andExpect(jsonPath("$.netResult").value(-442.5));
	}

	@Test
	void answersAnEmptyDreForAPeriodWithoutActivity() throws Exception {
		mockMvc.perform(get("/api/reports/dre").param("period", "2016-01").param("companyId", company.value().toString())
				.header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.grossRevenue").value(0))
				.andExpect(jsonPath("$.deductions").value(0))
				.andExpect(jsonPath("$.cmv").value(0))
				.andExpect(jsonPath("$.expensesByCostCenter.length()").value(0))
				.andExpect(jsonPath("$.netResult").value(0));
	}

	@Test
	void answersBadRequestWhenThePeriodIsMissingOrMalformed() throws Exception {
		mockMvc.perform(get("/api/reports/dre").header("Authorization", "Bearer valid-token"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/reports/dre").param("period", "May").header("Authorization", "Bearer valid-token"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void answersForbiddenWhenTheProfileCannotViewTheDre() throws Exception {
		when(sessionStorePort.resolve("other-token")).thenReturn(Optional.of(UserId.generate()));

		mockMvc.perform(get("/api/reports/dre").param("period", "2016-05").header("Authorization", "Bearer other-token"))
				.andExpect(status().isForbidden());
	}

	@Test
	void answersUnauthorizedWithoutASession() throws Exception {
		mockMvc.perform(get("/api/reports/dre").param("period", "2016-05")).andExpect(status().isUnauthorized());
	}

	private void payable(PayableOrigin origin, PayableStatus status, String amount, LocalDate dueDate,
			CostCenterShare... split) {
		payableRepositoryPort.save(Payable.of(PayableId.of(UUID.randomUUID()), null, origin, new BigDecimal(amount), dueDate,
				List.of(split), status, null, null, null, new LedgerScope(company.value(), null, null)));
	}

	private static Instant at(int month, int day, int hour, int minute) {
		return LocalDateTime.of(2016, month, day, hour, minute).atZone(ZONE).toInstant();
	}

	private NfseDocument nfse(Instant authorizedAt, String iss) {
		long rps = ++rpsNumber;
		return NfseDocument.issueRps(NfseId.of(UUID.randomUUID()), company, MUNICIPALITY,
				NfseTomador.of(null, "52998224725", PersonType.INDIVIDUAL, "Pessoa Fisica", null, null),
				ServiceCode.of("1.05"), PlaceOfProvision.PROVIDER, MUNICIPALITY, new BigDecimal("1000.00"),
				new BigDecimal("5.0000"), new BigDecimal(iss), null, List.of(), "Consultoria", "RPS", rps,
				at(5, 1, 8, 0)).convertToNfse("1", rps, at(5, 1, 9, 0)).send(at(5, 1, 10, 0))
				.authorize("protocol-" + rps, authorizedAt, "xml-ref");
	}
}
