package br.gravita.core.ports.outbound.persistence.tax;

/** A municipality's own list of service codes it accepts for NFSe (doc §5.2: "LC 116 list plus the municipal list"). */
public interface MunicipalServiceCodeRepositoryPort {

	boolean hasServiceCodeList(String municipalityIbgeCode);

	boolean existsByMunicipalityAndServiceCode(String municipalityIbgeCode, String serviceCode);
}
