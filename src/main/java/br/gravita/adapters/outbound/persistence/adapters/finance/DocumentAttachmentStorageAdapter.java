package br.gravita.adapters.outbound.persistence.adapters.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.DocumentAttachmentJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.finance.DocumentAttachmentJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.finance.AttachmentFile;
import br.gravita.core.domain.finance.PayableAttachment;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.ports.outbound.persistence.finance.DocumentAttachmentStoragePort;
import java.util.UUID;

@PersistenceAdapter
class DocumentAttachmentStorageAdapter implements DocumentAttachmentStoragePort {

	private final DocumentAttachmentJpaRepository jpaRepository;

	DocumentAttachmentStorageAdapter(DocumentAttachmentJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public PayableAttachment store(PayableId payableId, AttachmentFile file) {
		DocumentAttachmentJpaEntity entity = DocumentAttachmentJpaEntity.builder()
				.id(UUID.randomUUID())
				.payableId(payableId.value())
				.fileName(file.fileName())
				.contentType(file.contentType())
				.content(file.content())
				.build();
		entity.setNew(true);
		DocumentAttachmentJpaEntity saved = jpaRepository.save(entity);
		return new PayableAttachment(saved.getId().toString(), file.fileName(), file.contentType(),
				file.content().length);
	}
}
