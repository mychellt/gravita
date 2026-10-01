package br.gravita.adapters.outbound.persistence.entities.tax;

import br.gravita.adapters.outbound.persistence.entities.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Next NFSe number of one {@code (company, municipality)}; independent of {@code document_series}. */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "nfse_number_sequences",
		uniqueConstraints = @UniqueConstraint(name = "uk_nfse_number_sequences_scope",
				columnNames = { "company_id", "municipality_ibge" }))
public class NfseNumberSequenceJpaEntity extends AbstractEntity<UUID> {

	@Id
	private UUID id;

	@Column(name = "company_id", nullable = false)
	private UUID companyId;

	@Column(name = "municipality_ibge", nullable = false, length = 7)
	private String municipalityIbge;

	@Column(nullable = false, length = 10)
	private String series;

	@Column(name = "next_number", nullable = false)
	private Long nextNumber;
}
