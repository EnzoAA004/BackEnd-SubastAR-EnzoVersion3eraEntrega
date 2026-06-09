package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "medios_pago")
public class MedioPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "verificado", nullable = false)
    private boolean verificado;

    @Column(name = "eliminado", nullable = false)
    private boolean eliminado;

    @Column(name = "notificado_verificacion", nullable = false)
    private boolean notificadoVerificacion;

    @Column(name = "rechazado", nullable = false)
    private boolean rechazado;

    @Column(name = "notificado_rechazo", nullable = false)
    private boolean notificadoRechazo;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    public void prePersist() {
        if (creadoEn == null) creadoEn = LocalDateTime.now();
    }
}
