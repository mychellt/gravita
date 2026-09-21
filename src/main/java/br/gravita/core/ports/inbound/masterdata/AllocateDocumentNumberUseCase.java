package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.DocumentNumber;

public interface AllocateDocumentNumberUseCase {
	DocumentNumber execute(AllocateDocumentNumberCommand command);
}
