package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "cuentas_bancarias")
public class CuentaBancaria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "medio_pago_id", nullable = false, unique = true)
    private Integer medioPagoId;

    @Column(name = "nombre_banco", nullable = false)
    private String nombreBanco;

    @Column(name = "pais_banco", nullable = false)
    private String paisBanco;

    @Column(name = "cbu_iban", nullable = false)
    private String cbuIban;

    @Column(name = "fondos_reservados", nullable = false)
    private BigDecimal fondosReservados;
}
