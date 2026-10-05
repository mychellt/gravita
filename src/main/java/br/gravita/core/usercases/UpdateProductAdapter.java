package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.UpdateProductPort;
import br.gravita.core.ports.outbound.persistence.InventoryLotSerialRepositoryPort;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class UpdateProductAdapter implements UpdateProductPort {

	private final ProductRepositoryPort productRepositoryPort;
	private final InventoryLotSerialRepositoryPort inventoryLotSerialRepositoryPort;

	public UpdateProductAdapter(final ProductRepositoryPort productRepositoryPort,
			final InventoryLotSerialRepositoryPort inventoryLotSerialRepositoryPort) {
		this.productRepositoryPort = productRepositoryPort;
		this.inventoryLotSerialRepositoryPort = inventoryLotSerialRepositoryPort;
	}

	@Override
	public ProductDomain execute(final Context context) {
		final ProductDomain patch = context.getData(ProductDomain.class);
		final ProductDomain existing = productRepositoryPort.get(patch.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + patch.getId()));

		validateBarcodesAreUnique(patch, existing);
		validateLotSerialControlDowngrade(patch, existing);

		existing.applyPartialUpdate(patch);
		existing.validate();

		return productRepositoryPort.save(existing);
	}

	private void validateBarcodesAreUnique(final ProductDomain patch, final ProductDomain existing) {
		if (patch.getBarcodes() == null) {
			return;
		}
		for (final String barcode : patch.getBarcodes()) {
			final boolean alreadyOwnedByThisProduct = existing.getBarcodes() != null && existing.getBarcodes().contains(barcode);
			if (!alreadyOwnedByThisProduct && productRepositoryPort.existsByBarcode(barcode)) {
				throw new BusinessRuleException("Barcode is already registered to another product: " + barcode);
			}
		}
	}

	private void validateLotSerialControlDowngrade(final ProductDomain patch, final ProductDomain existing) {
		final boolean disablingLotControl = Boolean.TRUE.equals(existing.getLotControl()) && Boolean.FALSE.equals(patch.getLotControl());
		final boolean disablingSerialControl =
				Boolean.TRUE.equals(existing.getSerialControl()) && Boolean.FALSE.equals(patch.getSerialControl());
		if (!disablingLotControl && !disablingSerialControl) {
			return;
		}
		if (inventoryLotSerialRepositoryPort.hasOpenLotsOrSerials(existing.getId())) {
			throw new BusinessRuleException("Cannot disable lot/serial control while open lots or serials exist for this product");
		}
	}
}
