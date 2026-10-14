package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.ports.inbound.tax.ListInboundNfeUseCase;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.usercases.CallerCompanyResolver;
import java.util.List;

@UseCase
public class ListInboundNfeService implements ListInboundNfeUseCase {

	private final InboundNfeRepositoryPort inboundNfeRepositoryPort;
	private final CallerCompanyResolver callerCompanyResolver;

	public ListInboundNfeService(final InboundNfeRepositoryPort inboundNfeRepositoryPort,
			final CallerCompanyResolver callerCompanyResolver) {
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
		this.callerCompanyResolver = callerCompanyResolver;
	}

	@Override
	public List<InboundNfe> execute(final Context context) {
		return callerCompanyResolver.resolve(context)
				.map(CompanyId::of)
				.map(inboundNfeRepositoryPort::findAllByCompany)
				.orElse(List.of());
	}
}
