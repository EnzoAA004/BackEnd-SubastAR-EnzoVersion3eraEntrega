package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "registros_pendientes")
public class RegistroPendiente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "apellido", nullable = false)
    private String apellido;

    @Column(name = "domicilio", nullable = false)
    private String domicilio;

    @Column(name = "pais_numero", nullable = false)
    private Integer paisNumero;

    @Lob
    @Column(name = "foto_dni_frente")
    private byte[] fotoDniFrente;

    @Lob
    @Column(name = "foto_dni_dorso")
    private byte[] fotoDniDorso;

    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "codigo_verificacion")
    private String codigoVerificacion;

    @Column(name = "codigo_expires_at")
    private LocalDateTime codigoExpiresAt;

    @Column(name = "token_verificacion")
    private String tokenVerificacion;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    public void prePersist() {
        if (creadoEn == null) creadoEn = LocalDateTime.now();
        if (estado == null) estado = "pendiente_revision";
    }
}
