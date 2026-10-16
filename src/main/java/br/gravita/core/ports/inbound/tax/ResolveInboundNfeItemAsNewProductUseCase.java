package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.ProductDomain;

public interface ResolveInboundNfeItemAsNewProductUseCase {
	ProductDomain execute(ResolveInboundNfeItemAsNewProductCommand command);
}
