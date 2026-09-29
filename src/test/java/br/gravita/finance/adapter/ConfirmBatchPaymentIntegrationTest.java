package br.gravita.finance.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementMethod;
import br.gravita.core.ports.inbound.finance.BankReturnImportResult;
import br.gravita.core.ports.inbound.finance.ConfirmBatchPaymentCommand;
import br.gravita.core.ports.inbound.finance.ConfirmBatchPaymentUseCase;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.BankReturnLine;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ConfirmBatchPaymentIntegrationTest {

	private static final LocalDate PAID_AT = LocalDate.of(2026, 9, 25);

	@Autowired
	private ConfirmBatchPaymentUseCase confirmBatchPaymentUseCase;

	@Autowired
	private PayableRepositoryPort payableRepositoryPort;

	@Autowired
	private SettlementRepositoryPort settlementRepositoryPort;

	@MockitoBean
	private BankIntegrationPort bankIntegrationPort;

	private Payable savedApproved() {
		return payableRepositoryPort.save(Payable.createManual(PayableId.of(UUID.randomUUID()), null,
				new BigDecimal("100.00"), LocalDate.now().plusDays(5), null).approve(UUID.randomUUID()));
	}

	private BankReturnLine paid(int lineNumber, Payable payable) {
		return new BankReturnLine(lineNumber, payable.getId().value().toString(), true, new BigDecimal("100.00"),
				null, null, null, null, PAID_AT);
	}

	private BankReturnLine rejected(int lineNumber, Payable payable) {
		return new BankReturnLine(lineNumber, payable.getId().value().toString(), false, null, null, null, null,
				null, null, "Insufficient funds");
	}

	private BankReturnImportResult confirm(BankReturnLine... lines) {
		when(bankIntegrationPort.parseReturnFile(BankIntegration.ITAU, "cnab")).thenReturn(List.of(lines));
		return confirmBatchPaymentUseCase.execute(new ConfirmBatchPaymentCommand(BankIntegration.ITAU, "cnab"));
	}

	@Test
	void persistsThePayableSettlementAndPaidStatusAndRejectsWithoutPaying() {
		Payable confirmed = savedApproved();
		Payable refused = savedApproved();

		BankReturnImportResult result = confirm(paid(1, confirmed), rejected(2, refused));

		assertThat(result.settledCount()).isEqualTo(1);
		assertThat(result.rejectedLines()).singleElement()
				.satisfies(line -> assertThat(line.titleIdentifier()).isEqualTo(refused.getId().value().toString()));
		assertThat(payableRepositoryPort.findById(confirmed.getId())).get().extracting(Payable::getStatus)
				.isEqualTo(PayableStatus.PAID);
		assertThat(payableRepositoryPort.findById(refused.getId())).get().extracting(Payable::getStatus)
				.isEqualTo(PayableStatus.APPROVED);
		assertThat(settlementRepositoryPort.findByPayableId(confirmed.getId())).singleElement().satisfies(s -> {
			assertThat(s.getMethod()).isEqualTo(SettlementMethod.AUTOMATIC_CNAB);
			assertThat(s.getPayableId()).isEqualTo(confirmed.getId());
			assertThat(s.getAmount()).isEqualByComparingTo("100.00");
		});
		assertThat(settlementRepositoryPort.findByPayableId(refused.getId())).isEmpty();
	}

	@Test
	void reimportingTheSameFileIsReportedNotSettledTwice() {
		Payable payable = savedApproved();
		confirm(paid(1, payable));

		BankReturnImportResult second = confirm(paid(1, payable));

		assertThat(second.settledCount()).isZero();
		assertThat(second.unmatchedLines()).hasSize(1);
		List<Settlement> settlements = settlementRepositoryPort.findByPayableId(payable.getId());
		assertThat(settlements).hasSize(1);
	}
}
