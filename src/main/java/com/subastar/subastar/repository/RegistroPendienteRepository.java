package com.subastar.subastar.repository;

import com.subastar.subastar.model.RegistroPendiente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistroPendienteRepository extends JpaRepository<RegistroPendiente, Integer> {
    boolean existsByEmail(String email);
    Optional<RegistroPendiente> findByEmail(String email);
    Optional<RegistroPendiente> findByEmailAndEstado(String email, String estado);
    Optional<RegistroPendiente> findByTokenVerificacion(String token);
    Optional<RegistroPendiente> findByEmailAndCodigoVerificacion(String email, String codigo);
}
