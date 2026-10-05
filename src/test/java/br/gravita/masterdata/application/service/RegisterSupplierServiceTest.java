package br.gravita.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.ports.inbound.masterdata.RegisterSupplierCommand;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.usercases.RegisterSupplierService;
import br.gravita.core.domain.masterdata.Address;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.shared.Document;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
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
	@DisplayName("Registers a new supplier without the optional purchasing fields")
	void shouldRegisterNewSupplierWithoutOptionalPurchasingFields() {
		final RegisterSupplierService service = new RegisterSupplierService(supplierRepositoryPort);
		when(supplierRepositoryPort.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final RegisterSupplierCommand command = new RegisterSupplierCommand(VALID_CNPJ, "Acme Supplies",
				List.of(VALID_ADDRESS), List.of(), null, null, null, null);

		final SupplierId id = service.execute(command);

		assertThat(id).isNotNull();
		final ArgumentCaptor<Supplier> savedSupplier = ArgumentCaptor.forClass(Supplier.class);
		verify(supplierRepositoryPort).save(savedSupplier.capture());
		assertThat(savedSupplier.getValue().getId()).isEqualTo(id);
		assertThat(savedSupplier.getValue().getDocument()).isEqualTo(VALID_CNPJ);
	}

	@Test
	@DisplayName("Registers a new supplier with the purchasing fields when provided")
	void shouldRegisterNewSupplierWithPurchasingFieldsWhenProvided() {
		final RegisterSupplierService service = new RegisterSupplierService(supplierRepositoryPort);
		when(supplierRepositoryPort.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final RegisterSupplierCommand command = new RegisterSupplierCommand(VALID_CNPJ, "Acme Supplies",
				List.of(VALID_ADDRESS), List.of(), null, null, 5, "1102");

		service.execute(command);

		final ArgumentCaptor<Supplier> savedSupplier = ArgumentCaptor.forClass(Supplier.class);
		verify(supplierRepositoryPort).save(savedSupplier.capture());
		assertThat(savedSupplier.getValue().getAverageLeadTimeDays()).isEqualTo(5);
		assertThat(savedSupplier.getValue().getDefaultPurchaseCfop()).isEqualTo("1102");
	}
}
