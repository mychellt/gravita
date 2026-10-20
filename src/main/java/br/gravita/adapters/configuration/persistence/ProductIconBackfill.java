package br.gravita.adapters.configuration.persistence;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Stores the classified icon on products saved before icons existed, so the database holds it for every product. Idempotent. */
@Slf4j
@Component
public class ProductIconBackfill implements ApplicationRunner {

	private final ProductRepositoryPort productRepositoryPort;

	public ProductIconBackfill(final ProductRepositoryPort productRepositoryPort) {
		this.productRepositoryPort = productRepositoryPort;
	}

	@Override
	public void run(final ApplicationArguments args) {
		int updated = 0;
		for (final ProductDomain product : productRepositoryPort.findAll()) {
			if (product.getIcon() == null) {
				product.refreshIcon();
				productRepositoryPort.save(product);
				updated++;
			}
		}
		if (updated > 0) {
			log.info("Stored the classified icon on {} product(s)", updated);
		}
	}
}
