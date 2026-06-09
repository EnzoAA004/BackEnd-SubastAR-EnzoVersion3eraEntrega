package com.subastar.subastar.repository;

import com.subastar.subastar.model.Catalogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CatalogoRepository extends JpaRepository<Catalogo, Integer> {
    Optional<Catalogo> findBySubastaIdentificador(Integer subastaId);
    List<Catalogo> findAllBySubastaIdentificador(Integer subastaId);
}
