package com.subastar.subastar.repository;

import com.subastar.subastar.model.Pujo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PujoRepository extends JpaRepository<Pujo, Integer> {
    List<Pujo> findByItemIdentificadorOrderByIdentificadorDesc(Integer itemId);

    Optional<Pujo> findTopByItemIdentificadorOrderByImporteDesc(Integer itemId);

    @Query("""
        SELECT p FROM Pujo p
        WHERE p.asistente.cliente.identificador = :clienteId
        """)
    List<Pujo> findByClienteId(@Param("clienteId") Integer clienteId);

    @Query("""
        SELECT p FROM Pujo p
        WHERE p.asistente.cliente.identificador = :clienteId
          AND p.ganador = 'si'
        """)
    List<Pujo> findGanadoresByClienteId(@Param("clienteId") Integer clienteId);

    @Query("""
        SELECT p FROM Pujo p
        WHERE p.item.identificador = :itemId
          AND p.ganador = 'si'
        """)
    Optional<Pujo> findGanadorByItemId(@Param("itemId") Integer itemId);
}
