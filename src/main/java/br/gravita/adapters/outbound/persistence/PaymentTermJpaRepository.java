package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.entities.PaymentTermJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface PaymentTermJpaRepository extends JpaRepository<PaymentTermJpaEntity, UUID> {
}
