package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "subastadores")
public class Subastador {
    @Id
    @Column(name = "identificador")
    private Integer identificador;

    @Column(name = "matricula")
    private String matricula;

    @Column(name = "region")
    private String region;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identificador", insertable = false, updatable = false)
    private Persona persona;
}
