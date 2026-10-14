package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.ports.inbound.tax.ListNfeUseCase;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.usercases.CallerCompanyResolver;
import java.util.List;

@UseCase
public class ListNfeService implements ListNfeUseCase {

	private final NfeRepositoryPort nfeRepositoryPort;
	private final CallerCompanyResolver callerCompanyResolver;

	public ListNfeService(final NfeRepositoryPort nfeRepositoryPort, final CallerCompanyResolver callerCompanyResolver) {
		this.nfeRepositoryPort = nfeRepositoryPort;
		this.callerCompanyResolver = callerCompanyResolver;
	}

	@Override
	public List<NfeDocument> execute(final Context context) {
		return callerCompanyResolver.resolve(context)
				.map(CompanyId::of)
				.map(nfeRepositoryPort::findAllByCompany)
				.orElse(List.of());
	}
}
