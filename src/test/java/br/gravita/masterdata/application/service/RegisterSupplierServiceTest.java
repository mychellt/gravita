package br.gravita.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.masterdata.application.port.in.RegisterSupplierCommand;
import br.gravita.masterdata.application.port.out.SupplierRepositoryPort;
import br.gravita.masterdata.domain.model.Address;
import br.gravita.masterdata.domain.model.Supplier;
import br.gravita.masterdata.domain.model.SupplierId;
import br.gravita.shared.Document;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterSupplierServiceTest {

	private static final Document VALID_CNPJ = Document.cnpj("11222333000181");
	private static final Address VALID_ADDRESS =
			new Address("Rua Teste", "100", null, "Centro", "Sao Paulo", "SP", "01000-000");

	@Mock
	private SupplierRepositoryPort supplierRepositoryPort;

	@Test
	void shouldRegisterNewSupplierWithoutOptionalPurchasingFields() {
		RegisterSupplierService service = new RegisterSupplierService(supplierRepositoryPort);
		when(supplierRepositoryPort.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RegisterSupplierCommand command = new RegisterSupplierCommand(VALID_CNPJ, "Acme Supplies",
				List.of(VALID_ADDRESS), List.of(), null, null, null, null);

		SupplierId id = service.execute(command);

		assertThat(id).isNotNull();
		ArgumentCaptor<Supplier> savedSupplier = ArgumentCaptor.forClass(Supplier.class);
		verify(supplierRepositoryPort).save(savedSupplier.capture());
		assertThat(savedSupplier.getValue().getId()).isEqualTo(id);
		assertThat(savedSupplier.getValue().getDocument()).isEqualTo(VALID_CNPJ);
	}

	@Test
	void shouldRegisterNewSupplierWithPurchasingFieldsWhenProvided() {
		RegisterSupplierService service = new RegisterSupplierService(supplierRepositoryPort);
		when(supplierRepositoryPort.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RegisterSupplierCommand command = new RegisterSupplierCommand(VALID_CNPJ, "Acme Supplies",
				List.of(VALID_ADDRESS), List.of(), null, null, 5, "1102");

		service.execute(command);

		ArgumentCaptor<Supplier> savedSupplier = ArgumentCaptor.forClass(Supplier.class);
		verify(supplierRepositoryPort).save(savedSupplier.capture());
		assertThat(savedSupplier.getValue().getAverageLeadTimeDays()).isEqualTo(5);
		assertThat(savedSupplier.getValue().getDefaultPurchaseCfop()).isEqualTo("1102");
	}
}
