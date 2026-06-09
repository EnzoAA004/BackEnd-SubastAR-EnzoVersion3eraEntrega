package com.subastar.subastar.repository;

import com.subastar.subastar.model.Asistente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AsistenteRepository extends JpaRepository<Asistente, Integer> {
    Optional<Asistente> findByClienteIdentificadorAndSubastaIdentificador(Integer clienteId, Integer subastaId);
    List<Asistente> findByClienteIdentificador(Integer clienteId);

    @Query("SELECT COALESCE(MAX(a.numeroPostor), 0) FROM Asistente a WHERE a.subasta.identificador = :subastaId")
    int findMaxNumeroPostorBySubastaId(@Param("subastaId") Integer subastaId);

    @Query("SELECT COUNT(a) > 0 FROM Asistente a WHERE a.cliente.identificador = :clienteId AND a.subasta.estado = 'abierta' AND a.subasta.identificador <> :subastaId")
    boolean existsEnOtraSubastaAbierta(@Param("clienteId") Integer clienteId, @Param("subastaId") Integer subastaId);
}
