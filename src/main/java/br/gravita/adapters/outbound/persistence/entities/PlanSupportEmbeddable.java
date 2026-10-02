package br.gravita.adapters.outbound.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class PlanSupportEmbeddable {

	@Column(name = "support_email", nullable = false)
	private boolean email;

	@Column(name = "support_chat", nullable = false)
	private boolean chat;

	@Column(name = "support_telefone", nullable = false)
	private boolean telefone;

	@Column(name = "support_sla_horas", nullable = false)
	private Integer slaHoras;

	@Column(name = "support_horario_comercial", nullable = false)
	private boolean horarioComercial;

	@Column(name = "support_gerente_dedicado", nullable = false)
	private boolean gerenteDedicado;
}
