package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "productos")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer identificador;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "disponible")
    private String disponible;

    @Column(name = "descripcionCatalogo")
    private String descripcionCatalogo;

    @Column(name = "descripcionCompleta", nullable = false)
    private String descripcionCompleta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisor", nullable = false)
    private Empleado revisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "duenio", nullable = false)
    private Duenio duenio;

    @Column(name = "seguro")
    private String seguroNroPoliza;
}
