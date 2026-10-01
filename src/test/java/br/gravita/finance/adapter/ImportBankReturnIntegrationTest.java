package br.gravita.finance.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementMethod;
import br.gravita.core.ports.inbound.finance.BankReturnImportResult;
import br.gravita.core.ports.inbound.finance.ImportBankReturnCommand;
import br.gravita.core.ports.inbound.finance.ImportBankReturnUseCase;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.BankReturnLine;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ImportBankReturnIntegrationTest {

	private static final LocalDate PAID_AT = LocalDate.of(2026, 9, 25);

	@Autowired
	private ImportBankReturnUseCase importBankReturnUseCase;

	@Autowired
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Autowired
	private SettlementRepositoryPort settlementRepositoryPort;

	@MockitoBean
	private BankIntegrationPort bankIntegrationPort;

	private Receivable savedReceivable() {
		return receivableRepositoryPort.save(Receivable.createManual(ReceivableId.of(UUID.randomUUID()),
				UUID.randomUUID(), new BigDecimal("100.00"), LocalDate.now().plusDays(30), null));
	}

	private BankReturnLine paid(int lineNumber, Receivable receivable, String amount) {
		return new BankReturnLine(lineNumber, receivable.getId().value().toString(), true, new BigDecimal(amount),
				null, null, null, null, PAID_AT);
	}

	private BankReturnImportResult importLines(BankReturnLine... lines) {
		when(bankIntegrationPort.parseReturnFile(BankIntegration.ITAU, "cnab")).thenReturn(List.of(lines));
		return importBankReturnUseCase.execute(new ImportBankReturnCommand(BankIntegration.ITAU, "cnab"));
	}

	@Test
	@DisplayName("Persists the settlements and the receivable status across partial and final payments")
	void persistsTheSettlementsAndTheReceivableStatusAcrossPartialAndFinalPayments() {
		Receivable receivable = savedReceivable();

		importLines(paid(1, receivable, "40.00"));

		assertThat(receivableRepositoryPort.findById(receivable.getId())).get().extracting(Receivable::getStatus)
				.isEqualTo(ReceivableStatus.PARTIALLY_SETTLED);

		BankReturnImportResult result = importLines(paid(1, receivable, "60.00"));

		assertThat(result.settledCount()).isEqualTo(1);
		assertThat(receivableRepositoryPort.findById(receivable.getId())).get().extracting(Receivable::getStatus)
				.isEqualTo(ReceivableStatus.SETTLED);
		List<Settlement> settlements = settlementRepositoryPort.findByReceivableId(receivable.getId());
		assertThat(settlements).hasSize(2)
				.allSatisfy(s -> assertThat(s.getMethod()).isEqualTo(SettlementMethod.AUTOMATIC_CNAB));
		assertThat(settlements).extracting(Settlement::getAmount).usingComparatorForType(
				BigDecimal::compareTo, BigDecimal.class).containsExactlyInAnyOrder(new BigDecimal("40.00"),
						new BigDecimal("60.00"));
	}

	@Test
	@DisplayName("Reports a re-imported bank return file instead of settling it twice")
	void reimportingTheSameFileIsReportedNotSettledTwice() {
		Receivable receivable = savedReceivable();
		importLines(paid(1, receivable, "100.00"));

		BankReturnImportResult second = importLines(paid(1, receivable, "100.00"));

		assertThat(second.settledCount()).isZero();
		assertThat(second.unmatchedLines()).hasSize(1);
		assertThat(settlementRepositoryPort.findByReceivableId(receivable.getId())).hasSize(1);
	}
}
