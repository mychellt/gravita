package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.inbound.inventory.SetAllowNegativeStockCommand;
import br.gravita.core.ports.inbound.inventory.SetAllowNegativeStockUseCase;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;

@UseCase
public class SetAllowNegativeStockService implements SetAllowNegativeStockUseCase {

	private final ProductRepositoryPort productRepositoryPort;

	public SetAllowNegativeStockService(final ProductRepositoryPort productRepositoryPort) {
		this.productRepositoryPort = productRepositoryPort;
	}

	@Override
	public ProductDomain execute(final SetAllowNegativeStockCommand command) {
		final ProductDomain product = productRepositoryPort.get(command.productId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + command.productId()));
		product.setAllowNegativeStock(command.allowNegativeStock());
		return productRepositoryPort.save(product);
	}
}
