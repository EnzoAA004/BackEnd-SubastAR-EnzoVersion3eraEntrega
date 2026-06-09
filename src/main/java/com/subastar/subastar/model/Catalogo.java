package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "catalogos")
public class Catalogo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer identificador;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subasta")
    private Subasta subasta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable", nullable = false)
    private Empleado responsable;
}
