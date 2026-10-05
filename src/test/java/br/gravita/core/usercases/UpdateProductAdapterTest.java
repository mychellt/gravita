package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.outbound.persistence.InventoryLotSerialRepositoryPort;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateProductAdapterTest {

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	@Mock
	private InventoryLotSerialRepositoryPort inventoryLotSerialRepositoryPort;

	@DisplayName("Rejects updating a product that does not exist")
	@Test
	void shouldRejectUpdateWhenProductDoesNotExist() {
		final UpdateProductAdapter adapter = new UpdateProductAdapter(productRepositoryPort, inventoryLotSerialRepositoryPort);
		final UUID id = UUID.randomUUID();
		final ProductDomain patch = ProductDomain.builder().id(id).internalCode("SKU-2").build();
		when(productRepositoryPort.get(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(patch))).isInstanceOf(ResourceNotFoundException.class);
		verify(productRepositoryPort, never()).save(any());
	}

	@DisplayName("A partial update applies only the fields that were provided")
	@Test
	void shouldApplyOnlyProvidedFieldsOnPartialUpdate() {
		final UpdateProductAdapter adapter = new UpdateProductAdapter(productRepositoryPort, inventoryLotSerialRepositoryPort);
		final UUID id = UUID.randomUUID();
		final ProductDomain existing = ProductDomain.builder()
				.id(id)
				.type(ProductType.SIMPLE)
				.internalCode("SKU-1")
				.basePrice(java.math.BigDecimal.TEN)
				.status(ProductStatus.ACTIVE)
				.build();
		final ProductDomain patch = ProductDomain.builder().id(id).status(ProductStatus.OUT_OF_STOCK).build();
		when(productRepositoryPort.get(id)).thenReturn(Optional.of(existing));
		when(productRepositoryPort.save(any(ProductDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final ProductDomain updated = adapter.execute(new Context(patch));

		assertThat(updated.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
		assertThat(updated.getInternalCode()).isEqualTo("SKU-1");
		assertThat(updated.getBasePrice()).isEqualByComparingTo(java.math.BigDecimal.TEN);
	}

	@DisplayName("Rejects a barcode that already belongs to another product")
	@Test
	void shouldRejectBarcodeAlreadyOwnedByAnotherProduct() {
		final UpdateProductAdapter adapter = new UpdateProductAdapter(productRepositoryPort, inventoryLotSerialRepositoryPort);
		final UUID id = UUID.randomUUID();
		final ProductDomain existing = ProductDomain.builder().id(id).type(ProductType.SIMPLE).internalCode("SKU-1").build();
		final ProductDomain patch = ProductDomain.builder().id(id).barcodes(List.of("7891234567895")).build();
		when(productRepositoryPort.get(id)).thenReturn(Optional.of(existing));
		when(productRepositoryPort.existsByBarcode("7891234567895")).thenReturn(true);

		assertThatThrownBy(() -> adapter.execute(new Context(patch)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("7891234567895");
		verify(productRepositoryPort, never()).save(any());
	}

	@DisplayName("Allows a product to keep its own barcode when updating")
	@Test
	void shouldAllowKeepingItsOwnBarcodeUnchanged() {
		final UpdateProductAdapter adapter = new UpdateProductAdapter(productRepositoryPort, inventoryLotSerialRepositoryPort);
		final UUID id = UUID.randomUUID();
		final ProductDomain existing = ProductDomain.builder()
				.id(id)
				.type(ProductType.SIMPLE)
				.internalCode("SKU-1")
				.barcodes(List.of("7891234567895"))
				.build();
		final ProductDomain patch = ProductDomain.builder().id(id).barcodes(List.of("7891234567895")).build();
		when(productRepositoryPort.get(id)).thenReturn(Optional.of(existing));
		when(productRepositoryPort.save(any(ProductDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final ProductDomain updated = adapter.execute(new Context(patch));

		assertThat(updated.getBarcodes()).containsExactly("7891234567895");
		verify(productRepositoryPort, never()).existsByBarcode(any());
	}

	@DisplayName("Rejects disabling lot control while the product has open lots")
	@Test
	void shouldRejectDisablingLotControlWhenOpenLotsExist() {
		final UpdateProductAdapter adapter = new UpdateProductAdapter(productRepositoryPort, inventoryLotSerialRepositoryPort);
		final UUID id = UUID.randomUUID();
		final ProductDomain existing = ProductDomain.builder()
				.id(id)
				.type(ProductType.SIMPLE)
				.internalCode("SKU-1")
				.lotControl(true)
				.build();
		final ProductDomain patch = ProductDomain.builder().id(id).lotControl(false).build();
		when(productRepositoryPort.get(id)).thenReturn(Optional.of(existing));
		when(inventoryLotSerialRepositoryPort.hasOpenLotsOrSerials(id)).thenReturn(true);

		assertThatThrownBy(() -> adapter.execute(new Context(patch))).isInstanceOf(BusinessRuleException.class);
		verify(productRepositoryPort, never()).save(any());
	}

	@DisplayName("Allows disabling lot control when the product has no open lots")
	@Test
	void shouldAllowDisablingLotControlWhenNoOpenLotsExist() {
		final UpdateProductAdapter adapter = new UpdateProductAdapter(productRepositoryPort, inventoryLotSerialRepositoryPort);
		final UUID id = UUID.randomUUID();
		final ProductDomain existing = ProductDomain.builder()
				.id(id)
				.type(ProductType.SIMPLE)
				.internalCode("SKU-1")
				.lotControl(true)
				.build();
		final ProductDomain patch = ProductDomain.builder().id(id).lotControl(false).build();
		when(productRepositoryPort.get(id)).thenReturn(Optional.of(existing));
		when(inventoryLotSerialRepositoryPort.hasOpenLotsOrSerials(id)).thenReturn(false);
		when(productRepositoryPort.save(any(ProductDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final ProductDomain updated = adapter.execute(new Context(patch));

		assertThat(updated.getLotControl()).isFalse();
	}

	@DisplayName("An unrelated update does not change the product's tax profile fields")
	@Test
	void shouldNotChangeTaxProfileFieldsOnUnrelatedUpdate() {
		final UpdateProductAdapter adapter = new UpdateProductAdapter(productRepositoryPort, inventoryLotSerialRepositoryPort);
		final UUID id = UUID.randomUUID();
		final ProductDomain existing = ProductDomain.builder()
				.id(id)
				.type(ProductType.SIMPLE)
				.internalCode("SKU-1")
				.ncm("12345678")
				.build();
		final ProductDomain patch = ProductDomain.builder().id(id).basePrice(java.math.BigDecimal.ONE).build();
		when(productRepositoryPort.get(id)).thenReturn(Optional.of(existing));
		when(productRepositoryPort.save(any(ProductDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		adapter.execute(new Context(patch));

		final ArgumentCaptor<ProductDomain> captor = ArgumentCaptor.forClass(ProductDomain.class);
		verify(productRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getNcm()).isEqualTo("12345678");
	}
}
