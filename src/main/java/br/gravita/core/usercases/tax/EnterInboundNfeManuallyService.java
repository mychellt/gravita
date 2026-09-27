package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.ports.inbound.tax.EnterInboundNfeManuallyCommand;
import br.gravita.core.ports.inbound.tax.EnterInboundNfeManuallyUseCase;
import br.gravita.core.ports.inbound.tax.ManualInboundNfeData;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * UC-M2-09: {@code manualData} is self-contained (it carries its own
 * {@code accessKey}), so it's always sufficient on its own. A bare
 * {@code accessKey} with no {@code manualData} is the "just typed the key"
 * path - today there's no SEFAZ query integration to auto-resolve it
 * (unlike {@code SubmitToSefazPort}, which only submits/cancels), so that
 * path reports it can't be completed without manual data rather than
 * silently fabricating supplier/item/value data.
 */
@UseCase
public class EnterInboundNfeManuallyService implements EnterInboundNfeManuallyUseCase {

	private final InboundNfeRepositoryPort inboundNfeRepositoryPort;

	@Autowired
	public EnterInboundNfeManuallyService(InboundNfeRepositoryPort inboundNfeRepositoryPort) {
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
	}

	@Override
	public InboundNfe execute(EnterInboundNfeManuallyCommand command) {
		ManualInboundNfeData manualData = command.manualData();
		if (manualData != null) {
			return inboundNfeRepositoryPort.save(toInboundNfe(command));
		}
		if (command.accessKey() != null && !command.accessKey().isBlank()) {
			throw new BusinessRuleException(
					"NFe " + command.accessKey() + " could not be resolved from SEFAZ; manual data is required");
		}
		throw new BusinessRuleException("Either accessKey or manualData must be provided");
	}

	private InboundNfe toInboundNfe(EnterInboundNfeManuallyCommand command) {
		ManualInboundNfeData data = command.manualData();
		return InboundNfe.enteredManually(InboundNfeId.of(UUID.randomUUID()), command.companyId(), data.accessKey(),
				data.series(), data.number(), data.supplierDocument(), data.supplierName(), data.issuedAt(),
				data.items(), data.totals());
	}
}
