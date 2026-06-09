package com.subastar.subastar.repository;

import com.subastar.subastar.model.BienSolicitudArchivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BienSolicitudArchivoRepository extends JpaRepository<BienSolicitudArchivo, Integer> {
    List<BienSolicitudArchivo> findBySolicitudIdAndTipoArchivo(Integer solicitudId, String tipo);
    List<BienSolicitudArchivo> findBySolicitudId(Integer solicitudId);
    Optional<BienSolicitudArchivo> findByCodigoArchivo(String codigo);
    int countBySolicitudIdAndTipoArchivo(Integer solicitudId, String tipo);
}
