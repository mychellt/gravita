package br.gravita.core.usercases;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.inbound.masterdata.FindSupplierByDocumentUseCase;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import java.util.Optional;

@UseCase
public class FindSupplierByDocumentService implements FindSupplierByDocumentUseCase {

	private final SupplierRepositoryPort supplierRepositoryPort;

	public FindSupplierByDocumentService(final SupplierRepositoryPort supplierRepositoryPort) {
		this.supplierRepositoryPort = supplierRepositoryPort;
	}

	@Override
	public Optional<Supplier> execute(final String cnpj) {
		return supplierRepositoryPort.findByDocument(Document.cnpj(cnpj));
	}
}
