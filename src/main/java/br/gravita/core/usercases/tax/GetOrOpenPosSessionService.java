package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.PosSessionStatus;
import br.gravita.core.ports.inbound.tax.GetOrOpenPosSessionUseCase;
import br.gravita.core.ports.outbound.persistence.tax.PosSessionRepositoryPort;
import br.gravita.core.usercases.CallerCompanyResolver;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/** The PDV screen is one register per company ("Caixa 01"), so its register id is derived from the company. */
@UseCase
public class GetOrOpenPosSessionService implements GetOrOpenPosSessionUseCase {

	static final String DEFAULT_REGISTER = "caixa-01";

	private final PosSessionRepositoryPort posSessionRepositoryPort;
	private final CallerCompanyResolver callerCompanyResolver;

	public GetOrOpenPosSessionService(final PosSessionRepositoryPort posSessionRepositoryPort,
			final CallerCompanyResolver callerCompanyResolver) {
		this.posSessionRepositoryPort = posSessionRepositoryPort;
		this.callerCompanyResolver = callerCompanyResolver;
	}

	@Override
	public PosSessionId execute(final Context context) {
		final CompanyId companyId = callerCompanyResolver.resolve(context).map(CompanyId::of)
				.orElseThrow(() -> new BusinessRuleException("The logged user has no company to open a cash register for"));
		final UUID registerId = UUID.nameUUIDFromBytes((companyId.value() + ":" + DEFAULT_REGISTER).getBytes(StandardCharsets.UTF_8));
		return posSessionRepositoryPort.findByRegisterIdAndStatus(registerId, PosSessionStatus.OPEN)
				.map(PosSession::getId)
				.orElseGet(() -> posSessionRepositoryPort.save(PosSession.open(PosSessionId.of(UUID.randomUUID()), registerId,
						context.getCaller().value(), companyId, BigDecimal.ZERO, Instant.now())).getId());
	}
}
