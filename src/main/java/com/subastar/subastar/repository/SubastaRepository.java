package com.subastar.subastar.repository;

import com.subastar.subastar.model.Subasta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubastaRepository extends JpaRepository<Subasta, Integer> {

    @Query("""
        SELECT s FROM Subasta s
        WHERE (:estado IS NULL OR s.estado = :estado)
          AND (:categoria IS NULL OR s.categoria = :categoria)
        """)
    List<Subasta> findByFiltros(
            @Param("estado") String estado,
            @Param("categoria") String categoria);
}
