package com.subastar.subastar.repository;

import com.subastar.subastar.model.Multa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface MultaRepository extends JpaRepository<Multa, Integer> {
    List<Multa> findByClienteIdentificadorAndEstado(Integer clienteId, String estado);

    @Query("SELECT COALESCE(SUM(m.monto), 0) FROM Multa m WHERE m.cliente.identificador = :clienteId AND m.estado = 'pendiente'")
    BigDecimal sumMultasPendientesByClienteId(@Param("clienteId") Integer clienteId);
}
