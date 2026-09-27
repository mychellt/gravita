package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.VoidedNumberRange;

public interface VoidDocumentNumberRangeUseCase {

	VoidedNumberRange execute(VoidNumberRangeCommand command);
}
