package br.gravita.adapters.outbound.persistence.entities.finance;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "document_attachments")
public class DocumentAttachmentJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "payable_id", nullable = false)
	private UUID payableId;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(name = "content_type", nullable = false)
	private String contentType;

	@Column(name = "content", nullable = false, columnDefinition = "bytea")
	private byte[] content;
}
