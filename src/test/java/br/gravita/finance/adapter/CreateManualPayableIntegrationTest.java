package br.gravita.finance.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.domain.finance.CostCenterShare;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableOrigin;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.ports.inbound.finance.CreateManualPayableCommand;
import br.gravita.core.ports.inbound.finance.CreateManualPayableUseCase;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CreateManualPayableIntegrationTest {

	@Autowired
	private CreateManualPayableUseCase createManualPayableUseCase;

	@Autowired
	private PayableRepositoryPort payableRepositoryPort;

	@Test
	@DisplayName("Persists a manual payable without a supplier as open")
	void persistsAManualPayableWithoutSupplierAsOpen() {
		LocalDate dueDate = LocalDate.now().plusDays(15);

		Payable created = createManualPayableUseCase
				.execute(new CreateManualPayableCommand(null, new BigDecimal("980.50"), dueDate, null));

		Payable found = payableRepositoryPort.findById(created.getId()).orElseThrow();
		assertThat(found.getOrigin()).isEqualTo(PayableOrigin.MANUAL);
		assertThat(found.getStatus()).isEqualTo(PayableStatus.OPEN);
		assertThat(found.getSupplierId()).isNull();
		assertThat(found.getAmount()).isEqualByComparingTo("980.50");
		assertThat(found.getDueDate()).isEqualTo(dueDate);
		assertThat(found.getCostCenterSplit()).isEmpty();
	}

	@Test
	@DisplayName("Persists the cost center split preserving its order")
	void persistsTheCostCenterSplitInOrder() {
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();
		Payable payable = Payable.createManual(PayableId.of(UUID.randomUUID()), UUID.randomUUID(),
				new BigDecimal("300.00"), LocalDate.now().plusDays(5), List.of(
						new CostCenterShare(first, new BigDecimal("25.50")),
						new CostCenterShare(second, new BigDecimal("74.50"))));

		payableRepositoryPort.save(payable);

		Payable found = payableRepositoryPort.findById(payable.getId()).orElseThrow();
		assertThat(found.getSupplierId()).isEqualTo(payable.getSupplierId());
		assertThat(found.getCostCenterSplit()).extracting(CostCenterShare::costCenterId)
				.containsExactly(first, second);
		assertThat(found.getCostCenterSplit().get(0).percent()).isEqualByComparingTo("25.50");
	}
}
