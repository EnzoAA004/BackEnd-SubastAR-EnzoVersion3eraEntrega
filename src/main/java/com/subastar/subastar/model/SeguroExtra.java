package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "seguros_extra")
public class SeguroExtra {
    @Id
    @Column(name = "poliza_id")
    private String polizaId;

    @Column(name = "beneficiario_id")
    private Integer beneficiarioId;

    @Column(name = "vigencia_desde")
    private LocalDate vigenciaDesde;

    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;

    @Column(name = "cobertura")
    private String cobertura;

    @Column(name = "contacto_telefono")
    private String contactoTelefono;

    @Column(name = "contacto_email")
    private String contactoEmail;

    @Column(name = "contacto_web")
    private String contactoWeb;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poliza_id", insertable = false, updatable = false)
    private Seguro seguro;
}
