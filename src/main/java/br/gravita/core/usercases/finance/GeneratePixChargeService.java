package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.PixCharge;
import br.gravita.core.domain.finance.PixChargeId;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.ports.inbound.finance.GeneratePixChargeCommand;
import br.gravita.core.ports.inbound.finance.GeneratePixChargeUseCase;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.IssuedPixCharge;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.PixChargeIssueRequest;
import br.gravita.core.ports.outbound.persistence.finance.PixChargeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import java.time.Instant;
import java.util.UUID;

@UseCase
public class GeneratePixChargeService implements GeneratePixChargeUseCase {

	private final ReceivableRepositoryPort receivableRepositoryPort;
	private final PixChargeRepositoryPort pixChargeRepositoryPort;
	private final BankIntegrationPort bankIntegrationPort;

	public GeneratePixChargeService(ReceivableRepositoryPort receivableRepositoryPort,
			PixChargeRepositoryPort pixChargeRepositoryPort, BankIntegrationPort bankIntegrationPort) {
		this.receivableRepositoryPort = receivableRepositoryPort;
		this.pixChargeRepositoryPort = pixChargeRepositoryPort;
		this.bankIntegrationPort = bankIntegrationPort;
	}

	@Override
	public PixCharge execute(GeneratePixChargeCommand command) {
		Receivable receivable = receivableRepositoryPort.findById(ReceivableId.of(command.receivableId()))
				.orElseThrow(() -> new ResourceNotFoundException("Receivable not found: " + command.receivableId()));
		// Checked before the bank is contacted so a rejected request never creates a charge there.
		receivable.requireOpen();

		PixChargeId id = PixChargeId.of(UUID.randomUUID());
		IssuedPixCharge issued = bankIntegrationPort.issuePixCharge(new PixChargeIssueRequest(id.value(),
				receivable.getId().value(), receivable.getCustomerId(), receivable.getAmount(),
				receivable.getDueDate()));

		return pixChargeRepositoryPort.save(
				PixCharge.issue(id, receivable, issued.dynamicQrPayload(), issued.expiresAt(), Instant.now()));
	}
}
