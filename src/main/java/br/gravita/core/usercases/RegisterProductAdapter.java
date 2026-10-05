package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.KitComponentDomain;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.business.RegisterProductPort;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RegisterProductAdapter implements RegisterProductPort {

	private final ProductRepositoryPort productRepositoryPort;

	public RegisterProductAdapter(final ProductRepositoryPort productRepositoryPort) {
		this.productRepositoryPort = productRepositoryPort;
	}

	@Override
	public ProductDomain execute(final Context context) {
		final ProductDomain product = context.getData(ProductDomain.class);
		if (product.getId() == null) {
			product.setId(UUID.randomUUID());
		}
		product.setStatus(ProductStatus.ACTIVE);

		product.validate();
		validateBarcodesAreUnique(product.getBarcodes());
		validateKitComponentsExist(product);

		return productRepositoryPort.save(product);
	}

	private void validateBarcodesAreUnique(final List<String> barcodes) {
		if (barcodes == null) {
			return;
		}
		for (final String barcode : barcodes) {
			if (productRepositoryPort.existsByBarcode(barcode)) {
				throw new BusinessRuleException("Barcode is already registered to another product: " + barcode);
			}
		}
	}

	private void validateKitComponentsExist(final ProductDomain product) {
		if (product.getType() != ProductType.KIT) {
			return;
		}
		for (final KitComponentDomain component : product.getKitComponents()) {
			if (productRepositoryPort.get(component.productId()).isEmpty()) {
				throw new BusinessRuleException("Kit references a product that does not exist: " + component.productId());
			}
		}
	}
}
