package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "registroDeSubasta")
public class RegistroDeSubasta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer identificador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subasta", nullable = false)
    private Subasta subasta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "duenio", nullable = false)
    private Duenio duenio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "importe", nullable = false)
    private BigDecimal importe;

    @Column(name = "comision", nullable = false)
    private BigDecimal comision;
}
