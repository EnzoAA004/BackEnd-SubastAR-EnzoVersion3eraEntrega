package com.subastar.subastar.repository;

import com.subastar.subastar.model.CompraExtra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompraExtraRepository extends JpaRepository<CompraExtra, Integer> {
    Optional<CompraExtra> findByRegistroId(Integer registroId);
}
