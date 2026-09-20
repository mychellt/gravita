package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.persistence.InventoryLotSerialRepositoryPort;
import br.gravita.core.ports.persistence.ProductRepositoryPort;
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

	@Test
	void shouldRejectUpdateWhenProductDoesNotExist() {
		UpdateProductAdapter adapter = new UpdateProductAdapter(productRepositoryPort, inventoryLotSerialRepositoryPort);
		UUID id = UUID.randomUUID();
		ProductDomain patch = ProductDomain.builder().id(id).internalCode("SKU-2").build();
		when(productRepositoryPort.get(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(patch))).isInstanceOf(ResourceNotFoundException.class);
		verify(productRepositoryPort, never()).save(any());
	}

	@Test
	void shouldApplyOnlyProvidedFieldsOnPartialUpdate() {
		UpdateProductAdapter adapter = new UpdateProductAdapter(productRepositoryPort, inventoryLotSerialRepositoryPort);
		UUID id = UUID.randomUUID();
		ProductDomain existing = ProductDomain.builder()
				.id(id)
				.type(ProductType.SIMPLE)
				.internalCode("SKU-1")
				.basePrice(java.math.BigDecimal.TEN)
				.status(ProductStatus.ACTIVE)
				.build();
		ProductDomain patch = ProductDomain.builder().id(id).status(ProductStatus.OUT_OF_STOCK).build();
		when(productRepositoryPort.get(id)).thenReturn(Optional.of(existing));
		when(productRepositoryPort.save(any(ProductDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProductDomain updated = adapter.execute(new Context(patch));

		assertThat(updated.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
		assertThat(updated.getInternalCode()).isEqualTo("SKU-1");
		assertThat(updated.getBasePrice()).isEqualByComparingTo(java.math.BigDecimal.TEN);
	}

	@Test
	void shouldRejectBarcodeAlreadyOwnedByAnotherProduct() {
		UpdateProductAdapter adapter = new UpdateProductAdapter(productRepositoryPort, inventoryLotSerialRepositoryPort);
		UUID id = UUID.randomUUID();
		ProductDomain existing = ProductDomain.builder().id(id).type(ProductType.SIMPLE).internalCode("SKU-1").build();
		ProductDomain patch = ProductDomain.builder().id(id).barcodes(List.of("7891234567895")).build();
		when(productRepositoryPort.get(id)).thenReturn(Optional.of(existing));
		when(productRepositoryPort.existsByBarcode("7891234567895")).thenReturn(true);

		assertThatThrownBy(() -> adapter.execute(new Context(patch)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("7891234567895");
		verify(productRepositoryPort, never()).save(any());
	}

	@Test
	void shouldAllowKeepingItsOwnBarcodeUnchanged() {
		UpdateProductAdapter adapter = new UpdateProductAdapter(productRepositoryPort, inventoryLotSerialRepositoryPort);
		UUID id = UUID.randomUUID();
		ProductDomain existing = ProductDomain.builder()
				.id(id)
				.type(ProductType.SIMPLE)
				.internalCode("SKU-1")
				.barcodes(List.of("7891234567895"))
				.build();
		ProductDomain patch = ProductDomain.builder().id(id).barcodes(List.of("7891234567895")).build();
		when(productRepositoryPort.get(id)).thenReturn(Optional.of(existing));
		when(productRepositoryPort.save(any(ProductDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProductDomain updated = adapter.execute(new Context(patch));

		assertThat(updated.getBarcodes()).containsExactly("7891234567895");
		verify(productRepositoryPort, never()).existsByBarcode(any());
	}

	@Test
	void shouldRejectDisablingLotControlWhenOpenLotsExist() {
		UpdateProductAdapter adapter = new UpdateProductAdapter(productRepositoryPort, inventoryLotSerialRepositoryPort);
		UUID id = UUID.randomUUID();
		ProductDomain existing = ProductDomain.builder()
				.id(id)
				.type(ProductType.SIMPLE)
				.internalCode("SKU-1")
				.lotControl(true)
				.build();
		ProductDomain patch = ProductDomain.builder().id(id).lotControl(false).build();
		when(productRepositoryPort.get(id)).thenReturn(Optional.of(existing));
		when(inventoryLotSerialRepositoryPort.hasOpenLotsOrSerials(id)).thenReturn(true);

		assertThatThrownBy(() -> adapter.execute(new Context(patch))).isInstanceOf(BusinessRuleException.class);
		verify(productRepositoryPort, never()).save(any());
	}

	@Test
	void shouldAllowDisablingLotControlWhenNoOpenLotsExist() {
		UpdateProductAdapter adapter = new UpdateProductAdapter(productRepositoryPort, inventoryLotSerialRepositoryPort);
		UUID id = UUID.randomUUID();
		ProductDomain existing = ProductDomain.builder()
				.id(id)
				.type(ProductType.SIMPLE)
				.internalCode("SKU-1")
				.lotControl(true)
				.build();
		ProductDomain patch = ProductDomain.builder().id(id).lotControl(false).build();
		when(productRepositoryPort.get(id)).thenReturn(Optional.of(existing));
		when(inventoryLotSerialRepositoryPort.hasOpenLotsOrSerials(id)).thenReturn(false);
		when(productRepositoryPort.save(any(ProductDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProductDomain updated = adapter.execute(new Context(patch));

		assertThat(updated.getLotControl()).isFalse();
	}

	@Test
	void shouldNotChangeTaxProfileFieldsOnUnrelatedUpdate() {
		UpdateProductAdapter adapter = new UpdateProductAdapter(productRepositoryPort, inventoryLotSerialRepositoryPort);
		UUID id = UUID.randomUUID();
		ProductDomain existing = ProductDomain.builder()
				.id(id)
				.type(ProductType.SIMPLE)
				.internalCode("SKU-1")
				.ncm("12345678")
				.build();
		ProductDomain patch = ProductDomain.builder().id(id).basePrice(java.math.BigDecimal.ONE).build();
		when(productRepositoryPort.get(id)).thenReturn(Optional.of(existing));
		when(productRepositoryPort.save(any(ProductDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		adapter.execute(new Context(patch));

		ArgumentCaptor<ProductDomain> captor = ArgumentCaptor.forClass(ProductDomain.class);
		verify(productRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getNcm()).isEqualTo("12345678");
	}
}
