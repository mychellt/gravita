package br.gravita.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.Address;
import br.gravita.core.domain.masterdata.PixKey;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierNotFoundException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.inbound.masterdata.UpdateSupplierCommand;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.usercases.UpdateSupplierService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateSupplierServiceTest {

	private static final Document VALID_CNPJ = Document.cnpj("11222333000181");
	private static final Address VALID_ADDRESS =
			new Address("Rua Teste", "100", null, "Centro", "Sao Paulo", "SP", "01000-000");

	@Mock
	private SupplierRepositoryPort supplierRepositoryPort;

	private Supplier existingSupplier(SupplierId id) {
		return Supplier.of(id, VALID_CNPJ, "Acme Supplies", List.of(VALID_ADDRESS), List.of(), null, null, 5, "1102");
	}

	@Test
	void shouldKeepUnspecifiedFieldsOnPartialUpdate() {
		UpdateSupplierService service = new UpdateSupplierService(supplierRepositoryPort);
		SupplierId id = SupplierId.of(UUID.randomUUID());
		when(supplierRepositoryPort.findById(id)).thenReturn(Optional.of(existingSupplier(id)));
		when(supplierRepositoryPort.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UpdateSupplierCommand command = new UpdateSupplierCommand(id, null, null, null, null, null, null, null, "1401");

		service.execute(command);

		ArgumentCaptor<Supplier> savedSupplier = ArgumentCaptor.forClass(Supplier.class);
		verify(supplierRepositoryPort).save(savedSupplier.capture());
		Supplier saved = savedSupplier.getValue();
		assertThat(saved.getDefaultPurchaseCfop()).isEqualTo("1401");
		assertThat(saved.getName()).isEqualTo("Acme Supplies");
		assertThat(saved.getDocument()).isEqualTo(VALID_CNPJ);
		assertThat(saved.getAddresses()).containsExactly(VALID_ADDRESS);
		assertThat(saved.getAverageLeadTimeDays()).isEqualTo(5);
	}

	@Test
	void shouldUpdatePixKeyWithoutRequiringOtherFields() {
		UpdateSupplierService service = new UpdateSupplierService(supplierRepositoryPort);
		SupplierId id = SupplierId.of(UUID.randomUUID());
		when(supplierRepositoryPort.findById(id)).thenReturn(Optional.of(existingSupplier(id)));
		when(supplierRepositoryPort.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

		PixKey newPixKey = PixKey.of("supplier@example.com");
		UpdateSupplierCommand command =
				new UpdateSupplierCommand(id, null, null, null, null, null, newPixKey, null, null);

		service.execute(command);

		ArgumentCaptor<Supplier> savedSupplier = ArgumentCaptor.forClass(Supplier.class);
		verify(supplierRepositoryPort).save(savedSupplier.capture());
		assertThat(savedSupplier.getValue().getPixKey()).isEqualTo(newPixKey);
		assertThat(savedSupplier.getValue().getDefaultPurchaseCfop()).isEqualTo("1102");
	}

	@Test
	void shouldThrowWhenSupplierDoesNotExist() {
		UpdateSupplierService service = new UpdateSupplierService(supplierRepositoryPort);
		SupplierId id = SupplierId.of(UUID.randomUUID());
		when(supplierRepositoryPort.findById(id)).thenReturn(Optional.empty());

		UpdateSupplierCommand command = new UpdateSupplierCommand(id, null, "New Name", null, null, null, null, null, null);

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(SupplierNotFoundException.class);
	}
}
