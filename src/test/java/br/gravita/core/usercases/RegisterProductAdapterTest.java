package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.KitComponentDomain;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class RegisterProductAdapterTest {

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	@DisplayName("Registering a product assigns an id and sets its status to active")
	@Test
	void shouldAssignIdAndActivateStatusOnRegister() {
		final RegisterProductAdapter adapter = new RegisterProductAdapter(productRepositoryPort);
		final ProductDomain product = ProductDomain.builder().type(ProductType.SIMPLE).internalCode("SKU-1").build();
		when(productRepositoryPort.save(any(ProductDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final ProductDomain created = adapter.execute(new Context(product));

		assertThat(created.getId()).isNotNull();
		assertThat(created.getStatus()).isEqualTo(ProductStatus.ACTIVE);
		final ArgumentCaptor<ProductDomain> captor = ArgumentCaptor.forClass(ProductDomain.class);
		verify(productRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getInternalCode()).isEqualTo("SKU-1");
	}

	@DisplayName("Rejects registering a product whose barcode is already taken")
	@Test
	void shouldRejectDuplicateBarcode() {
		final RegisterProductAdapter adapter = new RegisterProductAdapter(productRepositoryPort);
		final ProductDomain product = ProductDomain.builder()
				.type(ProductType.SIMPLE)
				.internalCode("SKU-1")
				.barcodes(List.of("7891234567895"))
				.build();
		when(productRepositoryPort.existsByBarcode("7891234567895")).thenReturn(true);

		assertThatThrownBy(() -> adapter.execute(new Context(product)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("7891234567895");
		verify(productRepositoryPort, never()).save(any());
	}

	@DisplayName("Rejects a kit that is composed of a product that does not exist")
	@Test
	void shouldRejectKitComposingUnknownProduct() {
		final RegisterProductAdapter adapter = new RegisterProductAdapter(productRepositoryPort);
		final UUID missingComponentId = UUID.randomUUID();
		final ProductDomain kit = ProductDomain.builder()
				.type(ProductType.KIT)
				.internalCode("KIT-1")
				.kitComponents(List.of(new KitComponentDomain(missingComponentId, BigDecimal.ONE)))
				.build();
		when(productRepositoryPort.get(missingComponentId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.execute(new Context(kit)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining(missingComponentId.toString());
		verify(productRepositoryPort, never()).save(any());
	}

	@DisplayName("Registers a kit when all of its component products exist")
	@Test
	void shouldRegisterKitWhenAllComponentsExist() {
		final RegisterProductAdapter adapter = new RegisterProductAdapter(productRepositoryPort);
		final UUID componentId = UUID.randomUUID();
		final ProductDomain kit = ProductDomain.builder()
				.type(ProductType.KIT)
				.internalCode("KIT-1")
				.kitComponents(List.of(new KitComponentDomain(componentId, BigDecimal.ONE)))
				.build();
		when(productRepositoryPort.get(componentId))
				.thenReturn(Optional.of(ProductDomain.builder().id(componentId).build()));
		when(productRepositoryPort.save(any(ProductDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final ProductDomain created = adapter.execute(new Context(kit));

		assertThat(created.getStatus()).isEqualTo(ProductStatus.ACTIVE);
		verify(productRepositoryPort).save(any());
	}

	@DisplayName("Rejects registering a product with more than five images")
	@Test
	void shouldRejectMoreThanFiveImages() {
		final RegisterProductAdapter adapter = new RegisterProductAdapter(productRepositoryPort);
		final ProductDomain product = ProductDomain.builder()
				.type(ProductType.SIMPLE)
				.internalCode("SKU-1")
				.images(List.of("1", "2", "3", "4", "5", "6"))
				.build();

		assertThatThrownBy(() -> adapter.execute(new Context(product))).isInstanceOf(BusinessRuleException.class);
		verify(productRepositoryPort, never()).save(any());
	}
}
