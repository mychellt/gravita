package br.gravita.masterdata.adapter.out.persistence;

import br.gravita.masterdata.domain.model.SefazEnvironment;
import br.gravita.masterdata.domain.model.TaxRegime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "companies")
public class CompanyJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "UUID")
	private UUID id;

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
