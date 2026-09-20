package br.gravita.adapters.outbound.persistence.repositories;

import br.gravita.adapters.outbound.persistence.entities.PaymentMethodJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentMethodJpaRepository extends JpaRepository<PaymentMethodJpaEntity, UUID> {
}
