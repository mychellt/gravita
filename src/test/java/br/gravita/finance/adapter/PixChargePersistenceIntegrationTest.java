package br.gravita.finance.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.domain.finance.PixCharge;
import br.gravita.core.domain.finance.PixChargeId;
import br.gravita.core.domain.finance.PixChargeStatus;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.ports.inbound.finance.ConfirmPixPaymentCommand;
import br.gravita.core.ports.inbound.finance.ConfirmPixPaymentUseCase;
import br.gravita.core.ports.inbound.finance.ExpirePixChargesUseCase;
import br.gravita.core.ports.outbound.persistence.finance.PixChargeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.finance.PixPayloads;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PixChargePersistenceIntegrationTest {

	@Autowired
	private PixChargeRepositoryPort pixChargeRepositoryPort;

	@Autowired
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Autowired
	private ConfirmPixPaymentUseCase confirmPixPaymentUseCase;

	@Autowired
	private ExpirePixChargesUseCase expirePixChargesUseCase;

	private Receivable savedReceivable() {
		return receivableRepositoryPort.save(Receivable.createManual(ReceivableId.of(UUID.randomUUID()),
				UUID.randomUUID(), new BigDecimal("150.00"), LocalDate.now().plusDays(30), null));
	}

	private PixCharge savedCharge(Receivable receivable, Instant expiresAt) {
		return pixChargeRepositoryPort.save(PixCharge.of(PixChargeId.of(UUID.randomUUID()), receivable.getId(),
				PixPayloads.valid(), receivable.getAmount(), receivable.getDueDate(), expiresAt,
				PixChargeStatus.PENDING));
	}

	@Test
	void confirmingAPaymentPersistsThePaidChargeAndTheSettledReceivable() {
		Receivable receivable = savedReceivable();
		PixCharge charge = savedCharge(receivable, Instant.now().plusSeconds(3600));

		confirmPixPaymentUseCase.execute(new ConfirmPixPaymentCommand(charge.getId().value()));

		assertThat(pixChargeRepositoryPort.findById(charge.getId())).get().extracting(PixCharge::getStatus)
				.isEqualTo(PixChargeStatus.PAID);
		assertThat(receivableRepositoryPort.findById(receivable.getId())).get().extracting(Receivable::getStatus)
				.isEqualTo(ReceivableStatus.SETTLED);
	}

	@Test
	void expiryJobExpiresOnlyPendingChargesPastTheirExpiry() {
		Receivable receivable = savedReceivable();
		PixCharge overdue = savedCharge(receivable, Instant.now().minusSeconds(60));
		PixCharge live = savedCharge(receivable, Instant.now().plusSeconds(3600));

		expirePixChargesUseCase.execute();

		assertThat(pixChargeRepositoryPort.findById(overdue.getId())).get().extracting(PixCharge::getStatus)
				.isEqualTo(PixChargeStatus.EXPIRED);
		assertThat(pixChargeRepositoryPort.findById(live.getId())).get().extracting(PixCharge::getStatus)
				.isEqualTo(PixChargeStatus.PENDING);
		assertThat(pixChargeRepositoryPort.findByReceivableId(receivable.getId())).hasSize(2);
	}
}
