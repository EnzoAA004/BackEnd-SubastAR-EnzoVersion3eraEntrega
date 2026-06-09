package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "credenciales")
public class Credencial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "persona_id", nullable = false, unique = true)
    private Integer personaId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", insertable = false, updatable = false)
    private Persona persona;
}
