package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.sales.SalespersonTarget;
import br.gravita.core.ports.inbound.sales.SetSalespersonTargetCommand;
import br.gravita.core.ports.inbound.sales.SetSalespersonTargetUseCase;
import br.gravita.core.ports.outbound.persistence.sales.SalespersonTargetRepositoryPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class SetSalespersonTargetService implements SetSalespersonTargetUseCase {

	private final SalespersonTargetRepositoryPort salespersonTargetRepositoryPort;

	@Override
	public void execute(SetSalespersonTargetCommand command) {
		SalespersonTarget target = new SalespersonTarget(command.salesperson(), command.month(),
				command.valueTarget(), command.orderCountTarget());

		salespersonTargetRepositoryPort.save(target);
	}
}
