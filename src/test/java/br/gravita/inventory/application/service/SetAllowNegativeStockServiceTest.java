package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.inbound.inventory.SetAllowNegativeStockCommand;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.usercases.inventory.SetAllowNegativeStockService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SetAllowNegativeStockServiceTest {

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	private SetAllowNegativeStockService service;

	private final UUID productId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new SetAllowNegativeStockService(productRepositoryPort);
	}

	@Test
	@DisplayName("Rejects toggling a product that does not exist")
	void rejectsAnUnknownProduct() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new SetAllowNegativeStockCommand(productId, true)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(productId.toString());
		verify(productRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Persists the new value and leaves the rest of the product untouched")
	void persistsTheNewValue() {
		final ProductDomain existing = ProductDomain.builder().id(productId).internalCode("SKU-1")
				.allowNegativeStock(false).build();
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(existing));
		when(productRepositoryPort.save(any(ProductDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final ProductDomain updated = service.execute(new SetAllowNegativeStockCommand(productId, true));

		assertThat(updated.getAllowNegativeStock()).isTrue();
		assertThat(updated.getInternalCode()).isEqualTo("SKU-1");
	}
}
