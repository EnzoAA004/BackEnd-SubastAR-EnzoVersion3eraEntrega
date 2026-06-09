package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "itemsCatalogo")
public class ItemCatalogo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer identificador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalogo", nullable = false)
    private Catalogo catalogo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto", nullable = false)
    private Producto producto;

    @Column(name = "precioBase", nullable = false)
    private BigDecimal precioBase;

    @Column(name = "comision", nullable = false)
    private BigDecimal comision;

    @Column(name = "subastado")
    private String subastado;
}
