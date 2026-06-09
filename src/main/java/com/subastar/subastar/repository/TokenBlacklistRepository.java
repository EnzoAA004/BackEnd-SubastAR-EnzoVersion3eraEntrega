package com.subastar.subastar.repository;

import com.subastar.subastar.model.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Integer> {
    boolean existsByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenBlacklist t WHERE t.expiresAt < :now")
    void deleteExpired(LocalDateTime now);
}
