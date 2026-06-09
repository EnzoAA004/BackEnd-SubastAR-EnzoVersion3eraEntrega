package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "pujos")
public class Pujo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer identificador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asistente", nullable = false)
    private Asistente asistente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item", nullable = false)
    private ItemCatalogo item;

    @Column(name = "importe", nullable = false)
    private BigDecimal importe;

    @Column(name = "ganador")
    private String ganador;
}
