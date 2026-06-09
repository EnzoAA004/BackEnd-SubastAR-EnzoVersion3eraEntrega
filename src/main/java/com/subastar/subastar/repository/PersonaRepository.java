package com.subastar.subastar.repository;

import com.subastar.subastar.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaRepository extends JpaRepository<Persona, Integer> {
}
