package br.gravita.finance.application.service;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.finance.LedgerScope;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.finance.PayableListItem;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.usercases.CallerCompanyResolver;
import br.gravita.core.usercases.finance.ListPayablesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListPayablesServiceTest {

	@Mock
	private PayableRepositoryPort payableRepositoryPort;
	@Mock
	private SupplierRepositoryPort supplierRepositoryPort;
	@Mock
	private UserRepositoryPort userRepositoryPort;

	private ListPayablesService service;
	private final UserId callerId = UserId.generate();
	private final UUID companyId = UUID.randomUUID();
	private final UUID supplierId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new ListPayablesService(payableRepositoryPort, supplierRepositoryPort, new CallerCompanyResolver(userRepositoryPort));
	}

	@Test
	@DisplayName("Lists the company's payables with the supplier name, looking each supplier up once")
	void listsThePayablesWithTheSupplierName() {
		final Supplier supplier = mock(Supplier.class);
		when(supplier.getName()).thenReturn("Fornecedor Beta");
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().companyId(companyId).build()));
		when(payableRepositoryPort.findAllByCompany(companyId)).thenReturn(List.of(payable(supplierId), payable(supplierId), payable(null)));
		when(supplierRepositoryPort.findById(SupplierId.of(supplierId))).thenReturn(Optional.of(supplier));

		final List<PayableListItem> result = service.execute(new Context().withCaller(callerId));

		assertThat(result).extracting(PayableListItem::supplierName).containsExactly("Fornecedor Beta", "Fornecedor Beta", null);
		verify(supplierRepositoryPort, times(1)).findById(SupplierId.of(supplierId));
	}

	@Test
	@DisplayName("A payable whose supplier is unknown is kept, without a name")
	void keepsThePayableWhenTheSupplierIsUnknown() {
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().companyId(companyId).build()));
		when(payableRepositoryPort.findAllByCompany(companyId)).thenReturn(List.of(payable(supplierId)));
		when(supplierRepositoryPort.findById(SupplierId.of(supplierId))).thenReturn(Optional.empty());

		assertThat(service.execute(new Context().withCaller(callerId))).extracting(PayableListItem::supplierName).containsExactly((String) null);
	}

	private Payable payable(final UUID supplier) {
		return Payable.createManual(PayableId.of(UUID.randomUUID()), supplier, new BigDecimal("50.00"), LocalDate.of(2026, 10, 5), null)
				.inScope(new LedgerScope(companyId, null, null));
	}
}
