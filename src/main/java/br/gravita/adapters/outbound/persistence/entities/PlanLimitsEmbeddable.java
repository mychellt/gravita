package br.gravita.adapters.outbound.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Nullable columns: {@code null} means the limit is unlimited. */
@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class PlanLimitsEmbeddable {

	@Column(name = "limit_cnpjs")
	private Integer cnpjs;

	@Column(name = "limit_filiais")
	private Integer filiais;

	@Column(name = "limit_caixas_pdv")
	private Integer caixasPdv;

	@Column(name = "limit_usuarios")
	private Integer usuarios;
}
