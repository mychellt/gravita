package br.gravita.adapters.outbound.persistence.entities.tax;

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
@Table(name = "discrimination_templates")
public class DiscriminationTemplateJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "service_code", nullable = false, length = 5)
	private String serviceCode;

	@Column(name = "template_text", nullable = false, columnDefinition = "TEXT")
	private String templateText;
}
