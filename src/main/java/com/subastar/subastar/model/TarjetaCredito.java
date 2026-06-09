package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tarjetas_credito")
public class TarjetaCredito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "medio_pago_id", nullable = false, unique = true)
    private Integer medioPagoId;

    @Column(name = "numero_tarjeta_masked", nullable = false)
    private String numeroTarjetaMasked;

    @Column(name = "titular", nullable = false)
    private String titular;

    @Column(name = "vencimiento", nullable = false)
    private String vencimiento;

    @Column(name = "dni_titular", nullable = false)
    private String dniTitular;

    @Column(name = "es_internacional", nullable = false, columnDefinition = "BIT DEFAULT 0")
    private boolean esInternacional = false;
}
