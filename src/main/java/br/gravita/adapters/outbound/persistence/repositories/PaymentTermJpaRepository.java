package br.gravita.adapters.outbound.persistence.repositories;

import br.gravita.adapters.outbound.persistence.entities.PaymentTermJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentTermJpaRepository extends JpaRepository<PaymentTermJpaEntity, UUID> {
}
