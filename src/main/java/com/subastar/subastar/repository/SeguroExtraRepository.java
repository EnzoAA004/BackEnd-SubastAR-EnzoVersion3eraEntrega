package com.subastar.subastar.repository;

import com.subastar.subastar.model.SeguroExtra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeguroExtraRepository extends JpaRepository<SeguroExtra, String> {
    List<SeguroExtra> findByBeneficiarioId(Integer clienteId);
}
