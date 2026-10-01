package br.gravita.finance.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.domain.CostCenterDomain;
import br.gravita.core.domain.finance.CostCenterShare;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.ports.inbound.finance.SplitPayableByCostCenterCommand;
import br.gravita.core.ports.inbound.finance.SplitPayableByCostCenterUseCase;
import br.gravita.core.ports.outbound.persistence.CostCenterRepositoryPort;
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
class SplitPayableByCostCenterIntegrationTest {

	@Autowired
	private SplitPayableByCostCenterUseCase splitPayableByCostCenterUseCase;

	@Autowired
	private PayableRepositoryPort payableRepositoryPort;

	@Autowired
	private CostCenterRepositoryPort costCenterRepositoryPort;

	private UUID costCenter(String code) {
		return costCenterRepositoryPort.save(CostCenterDomain.builder().id(UUID.randomUUID()).code(code).name(code).active(true).build()).getId();
	}

	@Test
	@DisplayName("Replaces the persisted cost center split and preserves its order")
	void replacesThePersistedSplitPreservingOrder() {
		UUID first = costCenter("CC-A" + UUID.randomUUID().toString().substring(0, 6));
		UUID second = costCenter("CC-B" + UUID.randomUUID().toString().substring(0, 6));
		UUID third = costCenter("CC-C" + UUID.randomUUID().toString().substring(0, 6));
		Payable payable = Payable.createManual(PayableId.of(UUID.randomUUID()), null, new BigDecimal("300.00"),
				LocalDate.now().plusDays(5), List.of(new CostCenterShare(first, new BigDecimal("100"))));
		payableRepositoryPort.save(payable);

		Payable result = splitPayableByCostCenterUseCase.execute(new SplitPayableByCostCenterCommand(
				payable.getId().value(), List.of(new CostCenterShare(third, new BigDecimal("33.33")),
						new CostCenterShare(second, new BigDecimal("66.67")))));

		assertThat(result.getCostCenterSplit()).extracting(CostCenterShare::costCenterId)
				.containsExactly(third, second);
		Payable found = payableRepositoryPort.findById(payable.getId()).orElseThrow();
		assertThat(found.getCostCenterSplit()).extracting(CostCenterShare::costCenterId)
				.containsExactly(third, second);
		assertThat(found.getCostCenterSplit().get(0).percent()).isEqualByComparingTo("33.33");
		assertThat(found.getAmount()).isEqualByComparingTo("300.00");
	}
}
