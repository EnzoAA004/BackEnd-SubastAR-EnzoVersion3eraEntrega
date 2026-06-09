package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "subastas")
public class Subasta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer identificador;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "hora", nullable = false)
    private LocalTime hora;

    @Column(name = "estado")
    private String estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subastador")
    private Subastador subastador;

    @Column(name = "ubicacion")
    private String ubicacion;

    @Column(name = "capacidadAsistentes")
    private Integer capacidadAsistentes;

    @Column(name = "tieneDeposito")
    private String tieneDeposito;

    @Column(name = "seguridadPropia")
    private String seguridadPropia;

    @Column(name = "categoria")
    private String categoria;
}
