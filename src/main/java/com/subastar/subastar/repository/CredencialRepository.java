package com.subastar.subastar.repository;

import com.subastar.subastar.model.Credencial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredencialRepository extends JpaRepository<Credencial, Integer> {
    Optional<Credencial> findByEmail(String email);
    boolean existsByEmail(String email);
}
