package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "fotos")
public class Foto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer identificador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto", nullable = false)
    private Producto producto;

    @Lob
    @Column(name = "foto", nullable = false)
    private byte[] foto;
}
