package com.subastar.subastar.repository;

import com.subastar.subastar.model.CuentaBancaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuentaBancariaRepository extends JpaRepository<CuentaBancaria, Integer> {
    Optional<CuentaBancaria> findByMedioPagoId(Integer medioPagoId);
    boolean existsByCbuIban(String cbuIban);
}
