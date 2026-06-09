package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "paises")
public class Pais {
    @Id
    @Column(name = "numero")
    private Integer numero;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "nombreCorto")
    private String nombreCorto;

    @Column(name = "capital", nullable = false)
    private String capital;

    @Column(name = "nacionalidad", nullable = false)
    private String nacionalidad;

    @Column(name = "idiomas", nullable = false)
    private String idiomas;
}
