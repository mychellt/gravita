package br.gravita.finance.adapter;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.finance.CashMovement;
import br.gravita.core.domain.finance.CashMovementDirection;
import br.gravita.core.domain.finance.CashMovementId;
import br.gravita.core.domain.finance.InternalCashBoxId;
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
import br.gravita.core.ports.outbound.persistence.finance.InternalCashBoxRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ReconcileBankStatementEndToEndTest {

	// A day far from any other test's data, so movements recorded elsewhere cannot fall in the statement period.
	private static final LocalDate DAY = LocalDate.of(2031, 3, 14);
	private static final Instant NOON = DAY.atTime(12, 0).toInstant(ZoneOffset.UTC);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Autowired
	private PayableRepositoryPort payableRepositoryPort;

	@Autowired
	private SettlementRepositoryPort settlementRepositoryPort;

	@Autowired
	private InternalCashBoxRepositoryPort internalCashBoxRepositoryPort;

	private final UUID bankAccount = UUID.randomUUID();

	private Settlement receivedInto(UUID account, String amount) {
		Receivable receivable = receivableRepositoryPort.save(Receivable.of(ReceivableId.of(UUID.randomUUID()),
				UUID.randomUUID(), ReceivableOrigin.MANUAL, new BigDecimal(amount), DAY, null,
				ReceivableStatus.SETTLED, null, null, new LedgerScope(null, null, account)));
		return settlementRepositoryPort.save(Settlement.of(SettlementId.of(UUID.randomUUID()), receivable.getId(),
				new BigDecimal(amount), null, null, null, null, SettlementMethod.AUTOMATIC_CNAB, NOON));
	}

	private Settlement paidFrom(UUID account, String amount) {
		Payable payable = payableRepositoryPort.save(Payable.of(PayableId.of(UUID.randomUUID()), null,
				PayableOrigin.MANUAL, new BigDecimal(amount), DAY, List.of(), PayableStatus.PAID, null, null, null,
				new LedgerScope(null, null, account)));
		return settlementRepositoryPort.save(Settlement.ofPayable(SettlementId.of(UUID.randomUUID()),
				payable.getId(), new BigDecimal(amount), null, null, null, null, SettlementMethod.MANUAL, NOON));
	}

	private CashMovement depositOf(String amount) {
		return internalCashBoxRepositoryPort.saveMovement(CashMovement.of(CashMovementId.of(UUID.randomUUID()),
				InternalCashBoxId.MAIN, CashMovementDirection.TO_BANK, new BigDecimal(amount), "Deposit", NOON));
	}

	private ResultActions importStatement(UUID account, String fileContent) throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("bankAccount", account);
		body.put("fileContent", fileContent);
		return mockMvc.perform(post("/api/finance/bank-statements/import").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)));
	}

	@Test
	void matchesAStatementAgainstSettlementsAndCashMovementsAndReportsTheRest() throws Exception {
		Settlement received = receivedInto(bankAccount, "100.00");
		Settlement paid = paidFrom(bankAccount, "80.00");
		CashMovement deposit = depositOf("300.00");
		// Another account's settlement of the same value must not be offered to this statement.
		receivedInto(UUID.randomUUID(), "55.00");

		String csv = """
				date,amount,description
				2031-03-14,100.00,PIX RECEBIDO
				2031-03-14,-80.00,PAGAMENTO FORNECEDOR
				2031-03-14,300.00,DEPOSITO
				2031-03-14,55.00,PIX DE OUTRA CONTA
				2031-03-14,-12.34,TARIFA
				""";

		importStatement(bankAccount, csv).andExpect(status().isOk())
				.andExpect(jsonPath("$.matchedCount").value(3)).andExpect(jsonPath("$.unmatchedCount").value(2))
				.andExpect(jsonPath("$.matched", hasSize(3)))
				.andExpect(jsonPath("$.matched[0].lineNumber").value(2))
				.andExpect(jsonPath("$.matched[0].settlementId").value(received.getId().value().toString()))
				.andExpect(jsonPath("$.matched[0].cashMovementId").value(nullValue()))
				.andExpect(jsonPath("$.matched[1].settlementId").value(paid.getId().value().toString()))
				.andExpect(jsonPath("$.matched[2].cashMovementId").value(deposit.getId().value().toString()))
				.andExpect(jsonPath("$.matched[2].settlementId").value(nullValue()))
				.andExpect(jsonPath("$.unmatched[0].lineNumber").value(5))
				.andExpect(jsonPath("$.unmatched[0].description").value("PIX DE OUTRA CONTA"))
				.andExpect(jsonPath("$.unmatched[1].amount").value(-12.34))
				.andExpect(jsonPath("$.unmatched[1].settlementId").value(nullValue()));
	}

	@Test
	void matchesAnOfxStatement() throws Exception {
		Settlement received = receivedInto(bankAccount, "100.00");
		String ofx = "<OFX><BANKTRANLIST><STMTTRN><DTPOSTED>20310314</DTPOSTED><TRNAMT>100.00</TRNAMT>"
				+ "<FITID>1</FITID><MEMO>PIX</MEMO></STMTTRN></BANKTRANLIST></OFX>";

		importStatement(bankAccount, ofx).andExpect(status().isOk()).andExpect(jsonPath("$.unmatchedCount").value(0))
				.andExpect(jsonPath("$.matched[0].settlementId").value(received.getId().value().toString()))
				.andExpect(jsonPath("$.matched[0].reference").value("1"));
	}

	@Test
	void importingTheSameStatementTwiceGivesTheSameResult() throws Exception {
		receivedInto(bankAccount, "100.00");
		String csv = "date,amount\n2031-03-14,100.00\n";

		importStatement(bankAccount, csv).andExpect(status().isOk()).andExpect(jsonPath("$.matchedCount").value(1));
		importStatement(bankAccount, csv).andExpect(status().isOk()).andExpect(jsonPath("$.matchedCount").value(1));
	}

	@Test
	void anInvalidStatementIsRejectedWith400() throws Exception {
		importStatement(bankAccount, "date,description\n2031-03-14,x").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void aRequestWithoutABankAccountOrContentIsRejected() throws Exception {
		importStatement(null, "date,amount\n2031-03-14,1.00").andExpect(status().isBadRequest());
		importStatement(bankAccount, "  ").andExpect(status().isBadRequest());
		importStatement(bankAccount, null).andExpect(status().isBadRequest());
	}
}
