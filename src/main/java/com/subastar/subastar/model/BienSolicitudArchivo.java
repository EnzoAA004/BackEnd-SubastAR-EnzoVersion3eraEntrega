package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bien_solicitud_archivos")
public class BienSolicitudArchivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "codigo_archivo", nullable = false, unique = true)
    private String codigoArchivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private BienSolicitud solicitud;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "tipo_archivo", nullable = false)
    private String tipoArchivo;

    @Lob
    @Column(name = "datos")
    private byte[] datos;

    @Column(name = "url", nullable = true, length = 2048)
    private String url;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    public void prePersist() {
        if (creadoEn == null) creadoEn = LocalDateTime.now();
    }
}
