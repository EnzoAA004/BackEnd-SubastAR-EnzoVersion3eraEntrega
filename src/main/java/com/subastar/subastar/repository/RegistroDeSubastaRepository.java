package com.subastar.subastar.repository;

import com.subastar.subastar.model.RegistroDeSubasta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroDeSubastaRepository extends JpaRepository<RegistroDeSubasta, Integer> {
    List<RegistroDeSubasta> findByClienteIdentificador(Integer clienteId);
}
