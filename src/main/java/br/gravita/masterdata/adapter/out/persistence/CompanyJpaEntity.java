package br.gravita.masterdata.adapter.out.persistence;

import br.gravita.masterdata.domain.model.SefazEnvironment;
import br.gravita.masterdata.domain.model.TaxRegime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "companies")
public class CompanyJpaEntity implements Persistable<UUID> {

	@Id
	private UUID id;

	// id is always application-assigned (RegisterCompanyService); the repository
	// adapter sets this from existsById before save so Spring Data picks
	// persist() over merge() for a row that doesn't exist yet.
	@Transient
	@Builder.Default
	private boolean isNew = true;

	@Override
	public boolean isNew() {
		return isNew;
	}

	@Column(nullable = false, unique = true, length = 14)
	private String cnpj;

	@Column(nullable = false, length = 20)
	private String ie;

	@Column(nullable = false, length = 20)
	private String im;

	@Column(nullable = false, length = 10)
	private String cnae;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TaxRegime taxRegime;

	@Column(nullable = false)
	private boolean simplesOptante;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SefazEnvironment sefazEnvironment;

	@Column(nullable = false)
	private String address;

	@Column(nullable = false)
	private String issuingEmail;

	@Column(nullable = false, length = 20)
	private String phone;

	private String logoUrl;

	@Column(name = "parent_company_id")
	private UUID parentCompanyId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "modified_at", nullable = false)
	private LocalDateTime modifiedAt;
}
