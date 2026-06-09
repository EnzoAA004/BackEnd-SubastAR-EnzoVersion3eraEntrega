package com.subastar.subastar.repository;

import com.subastar.subastar.model.Foto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FotoRepository extends JpaRepository<Foto, Integer> {
    List<Foto> findByProductoIdentificador(Integer productoId);
}
