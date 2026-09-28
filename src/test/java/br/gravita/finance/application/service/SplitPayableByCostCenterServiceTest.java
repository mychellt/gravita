package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.CostCenterDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.CostCenterShare;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.SplitPayableByCostCenterCommand;
import br.gravita.core.ports.outbound.persistence.CostCenterRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.usercases.finance.SplitPayableByCostCenterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SplitPayableByCostCenterServiceTest {

	@Mock
	private PayableRepositoryPort payableRepositoryPort;

	@Mock
	private CostCenterRepositoryPort costCenterRepositoryPort;

	@InjectMocks
	private SplitPayableByCostCenterService service;

	private Payable existingPayable() {
		Payable payable = Payable.createManual(PayableId.of(UUID.randomUUID()), null, new BigDecimal("500.00"),
				LocalDate.now().plusDays(10), null);
		when(payableRepositoryPort.findById(payable.getId())).thenReturn(Optional.of(payable));
		return payable;
	}

	private static CostCenterShare share(UUID costCenterId, String percent) {
		return new CostCenterShare(costCenterId, new BigDecimal(percent));
	}

	private void costCenterExists(UUID id) {
		when(costCenterRepositoryPort.get(id)).thenReturn(Optional.of(CostCenterDomain.builder().build()));
	}

	@Test
	void savesThePayableWithTheNewSplit() {
		Payable payable = existingPayable();
		UUID a = UUID.randomUUID();
		UUID b = UUID.randomUUID();
		costCenterExists(a);
		costCenterExists(b);
		when(payableRepositoryPort.save(any(Payable.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Payable result = service.execute(new SplitPayableByCostCenterCommand(payable.getId().value(),
				List.of(share(a, "70"), share(b, "30"))));

		ArgumentCaptor<Payable> saved = ArgumentCaptor.forClass(Payable.class);
		verify(payableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo(payable.getId());
		assertThat(saved.getValue().getCostCenterSplit()).extracting(CostCenterShare::costCenterId)
				.containsExactly(a, b);
		assertThat(result.getCostCenterSplit()).hasSize(2);
	}

	@Test
	void rejectsAnUnknownPayable() {
		PayableId id = PayableId.of(UUID.randomUUID());
		when(payableRepositoryPort.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new SplitPayableByCostCenterCommand(id.value(),
				List.of(share(UUID.randomUUID(), "100"))))).isInstanceOf(ResourceNotFoundException.class);

		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsAnUnknownCostCenterWithoutSaving() {
		Payable payable = existingPayable();
		UUID unknown = UUID.randomUUID();
		when(costCenterRepositoryPort.get(unknown)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(
				new SplitPayableByCostCenterCommand(payable.getId().value(), List.of(share(unknown, "100")))))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsPercentagesNotSummingTo100WithoutSaving() {
		Payable payable = existingPayable();

		assertThatThrownBy(() -> service.execute(new SplitPayableByCostCenterCommand(payable.getId().value(),
				List.of(share(UUID.randomUUID(), "60"), share(UUID.randomUUID(), "30")))))
				.isInstanceOf(BusinessRuleException.class);

		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsAnEmptySplitWithoutSaving() {
		Payable payable = existingPayable();

		assertThatThrownBy(() -> service.execute(new SplitPayableByCostCenterCommand(payable.getId().value(), List.of())))
				.isInstanceOf(BusinessRuleException.class);

		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	void theCommandRequiresPayableIdAndSplit() {
		assertThatThrownBy(() -> new SplitPayableByCostCenterCommand(null, List.of()))
				.isInstanceOf(NullPointerException.class).hasMessageContaining("payableId");
		assertThatThrownBy(() -> new SplitPayableByCostCenterCommand(UUID.randomUUID(), null))
				.isInstanceOf(NullPointerException.class).hasMessageContaining("split");
	}
}
