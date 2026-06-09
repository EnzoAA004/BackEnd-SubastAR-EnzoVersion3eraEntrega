package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "asistentes")
public class Asistente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer identificador;

    @Column(name = "numeroPostor", nullable = false)
    private Integer numeroPostor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subasta", nullable = false)
    private Subasta subasta;
}
