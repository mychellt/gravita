package br.gravita.adapters.outbound.persistence.entities.masterdata;

import br.gravita.adapters.outbound.persistence.entities.PersonJpaEntity;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;


@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "companies")
@AttributeOverride(name = "document", column = @Column(name = "cnpj", nullable = false, unique = true, length = 14))
public class CompanyJpaEntity extends PersonJpaEntity {

	@Column(length = 20)
	private String ie;

	@Column(length = 20)
	private String im;

	@Column(length = 10)
	private String cnae;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TaxRegime taxRegime;

	@Column(nullable = false)
	private boolean simplesOptante;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SefazEnvironment sefazEnvironment;

	private String address;

	@Column(length = 2)
	private String state;

	private String issuingEmail;

	@Column(length = 20)
	private String phone;

	private String logoUrl;

	@Column(name = "parent_company_id")
	private UUID parentCompanyId;
}
