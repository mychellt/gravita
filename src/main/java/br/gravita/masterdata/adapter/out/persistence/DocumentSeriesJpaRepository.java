package br.gravita.masterdata.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentSeriesJpaRepository extends JpaRepository<DocumentSeriesJpaEntity, UUID> {
}
