package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "personas")
public class Persona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "identificador")
    private Integer identificador;

    @Column(name = "documento", nullable = false)
    private String documento;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "estado")
    private String estado;

    @Lob
    @Column(name = "foto")
    private byte[] foto;
}
