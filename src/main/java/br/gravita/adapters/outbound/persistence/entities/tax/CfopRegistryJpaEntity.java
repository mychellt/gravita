package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import br.gravita.core.domain.tax.NaturezaOperacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "cfop_registry")
public class CfopRegistryJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "natureza_operacao", nullable = false, length = 30)
	private NaturezaOperacao naturezaOperacao;

	@Column(name = "cfop", nullable = false, length = 4)
	private String cfop;

	@Column(name = "description", nullable = false)
	private String description;
}
