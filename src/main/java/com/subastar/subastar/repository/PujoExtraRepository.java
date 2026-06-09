package com.subastar.subastar.repository;

import com.subastar.subastar.model.PujoExtra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PujoExtraRepository extends JpaRepository<PujoExtra, Integer> {
    Optional<PujoExtra> findByPujoId(Integer pujoId);
}
