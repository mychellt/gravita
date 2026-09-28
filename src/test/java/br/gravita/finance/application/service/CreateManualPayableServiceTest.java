package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.CostCenterDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.CostCenterShare;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableOrigin;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.CreateManualPayableCommand;
import br.gravita.core.ports.outbound.persistence.CostCenterRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.usercases.finance.CreateManualPayableService;
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
class CreateManualPayableServiceTest {

	private static final LocalDate DUE = LocalDate.now().plusDays(30);

	@Mock
	private PayableRepositoryPort payableRepositoryPort;

	@Mock
	private SupplierRepositoryPort supplierRepositoryPort;

	@Mock
	private CostCenterRepositoryPort costCenterRepositoryPort;

	@InjectMocks
	private CreateManualPayableService service;

	private void savingEchoesTheArgument() {
		when(payableRepositoryPort.save(any(Payable.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void createsAnOpenManualPayableWithoutASupplier() {
		savingEchoesTheArgument();

		Payable created = service.execute(new CreateManualPayableCommand(null, new BigDecimal("2500.00"), DUE, null));

		assertThat(created.getOrigin()).isEqualTo(PayableOrigin.MANUAL);
		assertThat(created.getStatus()).isEqualTo(PayableStatus.OPEN);
		assertThat(created.getSupplierId()).isNull();
		assertThat(created.getAmount()).isEqualByComparingTo("2500.00");
		assertThat(created.getDueDate()).isEqualTo(DUE);
		verify(supplierRepositoryPort, never()).findById(any());
	}

	@Test
	void createsAPayableForARegisteredSupplier() {
		UUID supplierId = UUID.randomUUID();
		when(supplierRepositoryPort.findById(SupplierId.of(supplierId))).thenReturn(Optional.of(mock(Supplier.class)));
		savingEchoesTheArgument();

		Payable created = service.execute(new CreateManualPayableCommand(supplierId, BigDecimal.TEN, DUE, null));

		assertThat(created.getSupplierId()).isEqualTo(supplierId);
	}

	@Test
	void savesThePayableWithItsCostCenterSplit() {
		UUID a = UUID.randomUUID();
		UUID b = UUID.randomUUID();
		when(costCenterRepositoryPort.get(a)).thenReturn(Optional.of(CostCenterDomain.builder().build()));
		when(costCenterRepositoryPort.get(b)).thenReturn(Optional.of(CostCenterDomain.builder().build()));
		savingEchoesTheArgument();

		service.execute(new CreateManualPayableCommand(null, BigDecimal.TEN, DUE, List.of(
				new CostCenterShare(a, new BigDecimal("70")), new CostCenterShare(b, new BigDecimal("30")))));

		ArgumentCaptor<Payable> saved = ArgumentCaptor.forClass(Payable.class);
		verify(payableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getCostCenterSplit()).extracting(CostCenterShare::costCenterId)
				.containsExactly(a, b);
	}

	@Test
	void rejectsAnUnregisteredSupplierWithoutSavingAnything() {
		UUID supplierId = UUID.randomUUID();
		when(supplierRepositoryPort.findById(SupplierId.of(supplierId))).thenReturn(Optional.empty());

		assertThatThrownBy(
				() -> service.execute(new CreateManualPayableCommand(supplierId, BigDecimal.TEN, DUE, null)))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsAnUnknownCostCenterWithoutSavingAnything() {
		UUID costCenterId = UUID.randomUUID();
		when(costCenterRepositoryPort.get(costCenterId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new CreateManualPayableCommand(null, BigDecimal.TEN, DUE,
				List.of(new CostCenterShare(costCenterId, new BigDecimal("100"))))))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsASplitNotSummingTo100WithoutSavingAnything() {
		assertThatThrownBy(() -> service.execute(new CreateManualPayableCommand(null, BigDecimal.TEN, DUE,
				List.of(new CostCenterShare(UUID.randomUUID(), new BigDecimal("50"))))))
				.isInstanceOf(BusinessRuleException.class);

		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsAZeroAmountWithoutSavingAnything() {
		assertThatThrownBy(() -> service.execute(new CreateManualPayableCommand(null, BigDecimal.ZERO, DUE, null)))
				.isInstanceOf(BusinessRuleException.class);

		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	void theCommandRequiresAmountAndDueDate() {
		assertThatThrownBy(() -> new CreateManualPayableCommand(null, null, DUE, null))
				.isInstanceOf(NullPointerException.class).hasMessageContaining("amount");
		assertThatThrownBy(() -> new CreateManualPayableCommand(null, BigDecimal.TEN, null, null))
				.isInstanceOf(NullPointerException.class).hasMessageContaining("dueDate");
	}
}
