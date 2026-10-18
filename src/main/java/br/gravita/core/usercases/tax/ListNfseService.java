package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.IbgeMunicipalityDomain;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.ports.inbound.tax.ListNfseUseCase;
import br.gravita.core.ports.inbound.tax.NfseListItem;
import br.gravita.core.ports.outbound.persistence.IbgeMunicipalityRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import br.gravita.core.usercases.CallerCompanyResolver;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@UseCase
public class ListNfseService implements ListNfseUseCase {

	private final NfseRepositoryPort nfseRepositoryPort;
	private final IbgeMunicipalityRepositoryPort ibgeMunicipalityRepositoryPort;
	private final CallerCompanyResolver callerCompanyResolver;

	public ListNfseService(final NfseRepositoryPort nfseRepositoryPort,
			final IbgeMunicipalityRepositoryPort ibgeMunicipalityRepositoryPort,
			final CallerCompanyResolver callerCompanyResolver) {
		this.nfseRepositoryPort = nfseRepositoryPort;
		this.ibgeMunicipalityRepositoryPort = ibgeMunicipalityRepositoryPort;
		this.callerCompanyResolver = callerCompanyResolver;
	}

	@Override
	public List<NfseListItem> execute(final Context context) {
		final List<NfseDocument> documents = callerCompanyResolver.resolve(context)
				.map(CompanyId::of)
				.map(nfseRepositoryPort::findAllByProvider)
				.orElse(List.of());
		final Map<String, Optional<IbgeMunicipalityDomain>> municipalities = new HashMap<>();
		return documents.stream().map(document -> {
			final Optional<IbgeMunicipalityDomain> municipality = municipalities.computeIfAbsent(
					document.getIssMunicipalityIbgeCode(), ibgeMunicipalityRepositoryPort::findByIbgeCode);
			return new NfseListItem(document, municipality.map(IbgeMunicipalityDomain::getName).orElse(null),
					municipality.map(IbgeMunicipalityDomain::getStateCode).orElse(null));
		}).toList();
	}
}
