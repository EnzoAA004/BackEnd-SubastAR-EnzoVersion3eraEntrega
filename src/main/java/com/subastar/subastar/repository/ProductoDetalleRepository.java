package com.subastar.subastar.repository;

import com.subastar.subastar.model.ProductoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoDetalleRepository extends JpaRepository<ProductoDetalle, Integer> {
    List<ProductoDetalle> findByClienteId(Integer clienteId);
    List<ProductoDetalle> findByClienteIdAndEstadoSolicitud(Integer clienteId, String estado);
    boolean existsByClienteIdAndPolizaId(Integer clienteId, String polizaId);
    List<ProductoDetalle> findByClienteIdAndPolizaIdIsNotNull(Integer clienteId);
}
