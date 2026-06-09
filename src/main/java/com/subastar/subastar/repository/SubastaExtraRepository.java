package com.subastar.subastar.repository;

import com.subastar.subastar.model.SubastaExtra;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubastaExtraRepository extends JpaRepository<SubastaExtra, Integer> {
    Optional<SubastaExtra> findBySubastaId(Integer subastaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT se FROM SubastaExtra se WHERE se.subastaId = :subastaId")
    Optional<SubastaExtra> findBySubastaIdWithLock(@Param("subastaId") Integer subastaId);
}
