package br.gravita.core.usercases.tax;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeNotFoundException;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.usercases.CallerCompanyResolver;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Loads an inbound NFe only when it belongs to the caller's company; any other company's NFe looks nonexistent. */
@Component
public class OwnedInboundNfeLoader {

	private final InboundNfeRepositoryPort inboundNfeRepositoryPort;
	private final CallerCompanyResolver callerCompanyResolver;

	public OwnedInboundNfeLoader(final InboundNfeRepositoryPort inboundNfeRepositoryPort,
			final CallerCompanyResolver callerCompanyResolver) {
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
		this.callerCompanyResolver = callerCompanyResolver;
	}

	public InboundNfe load(final UUID inboundNfeId, final UserId callerId) {
		final CompanyId callerCompanyId = callerCompanyResolver.resolve(new Context().withCaller(callerId))
				.map(CompanyId::of)
				.orElseThrow(() -> new InboundNfeNotFoundException(inboundNfeId));
		return inboundNfeRepositoryPort.findById(InboundNfeId.of(inboundNfeId))
				.filter(inboundNfe -> inboundNfe.getCompanyId().equals(callerCompanyId))
				.orElseThrow(() -> new InboundNfeNotFoundException(inboundNfeId));
	}
}
