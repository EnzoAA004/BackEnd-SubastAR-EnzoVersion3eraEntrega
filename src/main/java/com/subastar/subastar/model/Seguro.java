package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "seguros")
public class Seguro {
    @Id
    @Column(name = "nroPoliza")
    private String nroPoliza;

    @Column(name = "compania", nullable = false)
    private String compania;

    @Column(name = "polizaCombinada")
    private String polizaCombinada;

    @Column(name = "importe", nullable = false)
    private BigDecimal importe;
}
