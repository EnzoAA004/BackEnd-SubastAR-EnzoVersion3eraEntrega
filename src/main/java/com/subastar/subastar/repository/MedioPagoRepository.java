package com.subastar.subastar.repository;

import com.subastar.subastar.model.MedioPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedioPagoRepository extends JpaRepository<MedioPago, Integer> {
    List<MedioPago> findByClienteIdentificadorAndEliminadoFalse(Integer clienteId);
    Optional<MedioPago> findByIdAndClienteIdentificadorAndEliminadoFalse(Integer id, Integer clienteId);
    List<MedioPago> findByVerificadoTrueAndEliminadoFalseAndNotificadoVerificacionFalse();
    List<MedioPago> findByRechazadoTrueAndNotificadoRechazoFalse();
}
