package br.gravita.reporting.adapter.in.web;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockMovementId;
import br.gravita.core.domain.inventory.StockMovementType;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockMovementRepositoryPort;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import br.gravita.core.usercases.system.CheckPermissionUseCase;
import java.math.BigDecimal;
import java.time.Instant;
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

/** Runs the real controller, service and read-model adapter over the real repositories; only session and permission are stubbed. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class GetStockTurnoverEndToEndTest {

	private static final UUID WAREHOUSE = UUID.randomUUID();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StockBalanceRepositoryPort stockBalanceRepositoryPort;

	@Autowired
	private StockMovementRepositoryPort stockMovementRepositoryPort;

	@MockitoBean
	private SessionStorePort sessionStorePort;

	@MockitoBean
	private CheckPermissionUseCase checkPermissionUseCase;

	private final UUID rice = UUID.randomUUID();
	private final UUID beans = UUID.randomUUID();

	@BeforeEach
	void seed() {
		UserId caller = UserId.generate();
		when(sessionStorePort.resolve("valid-token")).thenReturn(Optional.of(caller));
		when(checkPermissionUseCase.execute(argThat(
				query -> query != null && query.userId().equals(caller) && query.module().equals("reporting")
						&& query.screen().equals("stock-turnover")))).thenReturn(true);

		// Rice: 10 on hand on 2019-03-01, 30 bought and 20 sold in March, 5 more sold in April, 15 on hand today.
		balance(rice, "15");
		movement(rice, StockMovementType.ENTRY, "30", "2019-03-05T10:00:00Z");
		movement(rice, StockMovementType.EXIT, "20", "2019-03-20T10:00:00Z");
		movement(rice, StockMovementType.EXIT, "5", "2019-04-02T10:00:00Z");
		// Beans: 40 on hand throughout, nothing issued.
		balance(beans, "40");
	}

	@Test
	void reportsTheTurnoverOfTheMonthAndFlagsTheStalledProducts() throws Exception {
		// Rice opens at 10 and closes at 20: issued 20 over an average of 15.
		mockMvc.perform(get("/api/reports/stock-turnover").param("period", "2019-03")
				.header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.product == '" + rice + "')].turnoverRate").value(1.3333))
				.andExpect(jsonPath("$[?(@.product == '" + rice + "')].stalledFlag").value(false))
				.andExpect(jsonPath("$[?(@.product == '" + beans + "')].turnoverRate").value(0.0))
				.andExpect(jsonPath("$[?(@.product == '" + beans + "')].stalledFlag").value(true));
	}

	@Test
	void ignoresMovementsOutsideTheRequestedMonth() throws Exception {
		// In April rice opens at 20 and closes at 15: issued 5 over an average of 17.5.
		mockMvc.perform(get("/api/reports/stock-turnover").param("period", "2019-04")
				.header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.product == '" + rice + "')].turnoverRate").value(0.2857));
	}

	@Test
	void answersBadRequestWhenThePeriodIsMissingOrMalformed() throws Exception {
		mockMvc.perform(get("/api/reports/stock-turnover").header("Authorization", "Bearer valid-token"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/reports/stock-turnover").param("period", "March")
				.header("Authorization", "Bearer valid-token")).andExpect(status().isBadRequest());
	}

	@Test
	void answersForbiddenWhenTheProfileCannotViewTheReport() throws Exception {
		when(sessionStorePort.resolve("other-token")).thenReturn(Optional.of(UserId.generate()));

		mockMvc.perform(get("/api/reports/stock-turnover").param("period", "2019-03")
				.header("Authorization", "Bearer other-token")).andExpect(status().isForbidden());
	}

	@Test
	void answersUnauthorizedWithoutASession() throws Exception {
		mockMvc.perform(get("/api/reports/stock-turnover").param("period", "2019-03"))
				.andExpect(status().isUnauthorized());
	}

	private void balance(UUID product, String onHand) {
		stockBalanceRepositoryPort.save(StockBalance.of(StockBalanceId.of(UUID.randomUUID()), product, WAREHOUSE,
				new BigDecimal(onHand), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN));
	}

	private void movement(UUID product, StockMovementType type, String quantity, String at) {
		stockMovementRepositoryPort.save(StockMovement.of(StockMovementId.of(UUID.randomUUID()), type, product,
				WAREHOUSE, new BigDecimal(quantity), BigDecimal.TEN, null, List.of(), "TEST", null, UUID.randomUUID(),
				Instant.parse(at)));
	}
}
