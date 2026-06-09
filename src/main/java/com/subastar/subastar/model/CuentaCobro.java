package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "cuentas_cobro")
public class CuentaCobro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "cliente_id", nullable = false)
    private Integer clienteId;

    @Column(name = "nombre_banco", nullable = false)
    private String nombreBanco;

    @Column(name = "cbu_iban", nullable = false)
    private String cbuIban;

    @Column(name = "pais", nullable = false)
    private String pais;

    @Column(name = "moneda", nullable = false)
    private String moneda;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    public void prePersist() {
        if (creadoEn == null) creadoEn = LocalDateTime.now();
    }
}
